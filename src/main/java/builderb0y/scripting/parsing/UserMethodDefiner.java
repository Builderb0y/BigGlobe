package builderb0y.scripting.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.MethodDeclarationInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.UserScriptEnvironment;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList.UserParameter;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class UserMethodDefiner {

	public final ExpressionParser parser;
	public final String methodName;
	public final TypeInfo returnType;
	public UserParameterList userParameters;
	public List<VarInfo> newParameters;
	public MutableScriptEnvironment userParametersEnvironment;
	public UserScriptEnvironment userVariablesEnvironment;
	public int currentOffset;
	public MethodCompileContext newMethod;

	public UserMethodDefiner(ExpressionParser parser, String name, TypeInfo type) {
		this.parser = parser;
		this.methodName = name;
		this.returnType = type;
		this.userParametersEnvironment = new MutableScriptEnvironment().addAll(parser.environment.mutable());
		this.userVariablesEnvironment = new UserScriptEnvironment(parser.environment.user());
		this.userVariablesEnvironment.variables.clear();
		this.currentOffset = parser.method.info.isStatic() ? 0 : 1;
	}

	public InsnTree parse() throws ScriptParsingException {
		this.userParameters = UserParameterList.parse(this.parser);
		this.newParameters = new ArrayList<>(this.parser.method.parameters.size() + this.userParameters.parameters().length);
		this.addBuiltinParameters();
		this.addCapturedParameters();
		this.addUserParameters();
		this.setupMethod();
		this.makeMethodCallable();
		return this.parseMethodBody();
	}

	public void addBuiltinParameters() {
		for (VarInfo builtin : this.parser.method.parameters.values()) {
			if (builtin.index != this.currentOffset) {
				throw new IllegalStateException("Builtin parameter has incorrect offset: " + builtin + " should be at index " + this.currentOffset);
			}
			this.newParameters.add(builtin);
			this.currentOffset += builtin.type.getSize();
		}
	}

	public void capture(VarInfo captured) {
		this.newParameters.add(captured);
		InsnTree loader = load(captured);
		//must force put in backing map directly,
		//as normally this variable is "already defined".
		this.userParametersEnvironment.variables.put(captured.name, (parser, name) -> loader);
		this.currentOffset += captured.type.getSize();
	}

	public void addCapturedParameters() {
		for (VarInfo captured : this.parser.environment.user().getVariables()) {
			this.capture(new VarInfo(captured.name, this.currentOffset, captured.type));
		}
	}

	public void addUserParameters() {
		for (UserParameter userParameter : this.userParameters.parameters()) {
			VarInfo variable = new VarInfo(userParameter.name(), this.currentOffset, userParameter.type());
			this.newParameters.add(variable);
			this.userParametersEnvironment.addVariableLoad(variable);
			this.currentOffset += variable.type.getSize();
		}
	}

	public void setupMethod() {
		this.newMethod = this.parser.clazz.newMethod(
			this.parser.method.info.access(),
			this.methodName + '_' + this.parser.clazz.memberUniquifier++,
			this.returnType,
			this
			.newParameters
			.stream()
			.map(var -> var.type)
			.toArray(TypeInfo.ARRAY_FACTORY)
		);
		this.newMethod.scopes.pushScope();
		if (!this.newMethod.info.isStatic()) {
			this.newMethod.addThis();
		}
		for (VarInfo parameter : this.newParameters) {
			VarInfo added = this.newMethod.newParameter(parameter.name, parameter.type);
			if (added.index != parameter.index) {
				throw new IllegalStateException("Parameter index mismatch: " + parameter + " -> " + added);
			}
		}
	}

	public void makeMethodCallable() {
		this.makeMethodCallable(
			Stream.concat(
				this.parser.method.parameters.values().stream(),
				this.parser.environment.user().streamVariables()
			)
			.map(InsnTrees::load)
			.toArray(InsnTree.ARRAY_FACTORY),

			Arrays
			.stream(this.userParameters.parameters())
			.map(UserParameter::type)
			.toArray(TypeInfo.ARRAY_FACTORY)
		);
	}

	public abstract void makeMethodCallable(InsnTree[] implicitParameters, TypeInfo[] expectedTypes);

	public InsnTree parseMethodBody() throws ScriptParsingException {
		ExpressionParser newParser = new ExpressionParser(this.parser, this.newMethod);
		this.userVariablesEnvironment.parser = newParser;
		newParser.environment.user(this.userVariablesEnvironment).mutable(this.userParametersEnvironment);
		InsnTree result = newParser.parseRemainingInput(true);

		return new MethodDeclarationInsnTree(this.newMethod, result);
	}

	public static class UserFunctionDefiner extends UserMethodDefiner {

		public UserFunctionDefiner(ExpressionParser parser, String name, TypeInfo type) {
			super(parser, name, type);
		}

		@Override
		public void makeMethodCallable(InsnTree[] implicitParameters, TypeInfo[] expectedTypes) {
			MethodInfo newMethodInfo = this.newMethod.info;
			TypeInfo callerInfo = this.parser.clazz.info;
			FunctionHandler handler = (
				this.parser.method.info.isStatic()
				? (parser, name, arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, expectedTypes, CastMode.IMPLICIT_THROW, arguments);
					InsnTree[] concatenatedArguments = ObjectArrays.concat(implicitParameters, castArguments, InsnTree.class);
					return new CastResult(invokeStatic(newMethodInfo, concatenatedArguments), castArguments != arguments);
				}
				: (parser, name, arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, expectedTypes, CastMode.IMPLICIT_THROW, arguments);
					InsnTree[] concatenatedArguments = ObjectArrays.concat(implicitParameters, castArguments, InsnTree.class);
					return new CastResult(invokeInstance(load("this", 0, callerInfo), newMethodInfo, concatenatedArguments), castArguments != arguments);
				}
			);
			this.parser.environment.user().addFunction(this.methodName, handler);
			this.userVariablesEnvironment.addFunction(this.methodName, handler);
		}
	}

	public static class UserExtensionMethodDefiner extends UserMethodDefiner {

		public final TypeInfo typeBeingExtended;

		public UserExtensionMethodDefiner(ExpressionParser parser, String name, TypeInfo type, TypeInfo typeBeingExtended) {
			super(parser, name, type);
			this.typeBeingExtended = typeBeingExtended;
		}

		@Override
		public void addUserParameters() {
			this.capture(new VarInfo("this", this.currentOffset, this.typeBeingExtended));
			super.addUserParameters();
		}

		@Override
		public void makeMethodCallable(InsnTree[] implicitParameters, TypeInfo[] expectedTypes) {
			MethodInfo newMethodInfo = this.newMethod.info;
			TypeInfo callerInfo = this.parser.clazz.info;
			MethodHandler handler = (
				this.parser.method.info.isStatic()
				? (parser, receiver, name, mode, arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, expectedTypes, CastMode.IMPLICIT_THROW, arguments);
					InsnTree[] concatenatedArguments = concat(implicitParameters, receiver, castArguments);
					return new CastResult(invokeStatic(newMethodInfo, concatenatedArguments), castArguments != arguments);
				}
				: (parser, receiver, name, mode, arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, expectedTypes, CastMode.IMPLICIT_THROW, arguments);
					InsnTree[] concatenatedArguments = concat(implicitParameters, receiver, castArguments);
					return new CastResult(invokeInstance(load("this", 0, callerInfo), newMethodInfo, concatenatedArguments), castArguments != arguments);
				}
			);
			this.parser.environment.user().addMethod(this.typeBeingExtended, this.methodName, handler);
			this.userVariablesEnvironment.addMethod(this.typeBeingExtended, this.methodName, handler);
		}

		public static InsnTree[] concat(InsnTree[] implicit, InsnTree receiver, InsnTree[] arguments) {
			InsnTree[] result = new InsnTree[implicit.length + 1 + arguments.length];
			System.arraycopy(implicit, 0, result, 0, implicit.length);
			result[implicit.length] = receiver;
			System.arraycopy(arguments, 0, result, implicit.length + 1, arguments.length);
			return result;
		}
	}
}