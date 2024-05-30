package builderb0y.scripting.parsing;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList.UserParameter;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class UserMethodDefiner extends VariableCapturer {

	public final String methodName;
	public final TypeInfo returnType;

	public UserParameterList userParameters;
	public MethodInfo newMethod;

	public UserMethodDefiner(ExpressionParser parser, String methodName, TypeInfo returnType) {
		super(parser);
		this.methodName = methodName;
		this.returnType = returnType;
	}

	public void parse() throws ScriptParsingException {
		this.userParameters = UserParameterList.parse(this.parser);
		this.addBuiltinParameters();
		this.addCapturedParameters();
		this.newMethod = this.createMethodInfo();
		this.makeMethodCallable();
		this.parseMethodBody();
	}

	public Stream<TypeInfo> streamUserParameterTypes() {
		return (
			Arrays
			.stream(this.userParameters.parameters())
			.map(UserParameter::type)
		);
	}

	public Stream<LazyVarInfo> streamUserParameters() {
		return (
			Arrays
			.stream(this.userParameters.parameters())
			.map((UserParameter parameter) -> (
				new LazyVarInfo(parameter.name(), parameter.type())
			))
		);
	}

	public MethodInfo createMethodInfo(Stream<TypeInfo> parameterTypes) {
		return new MethodInfo(
			this.parser.method.info.access(),
			this.parser.clazz.info,
			this.methodName + '_' + this.parser.clazz.memberUniquifier++,
			this.returnType,
			parameterTypes.toArray(TypeInfo.ARRAY_FACTORY)
		);
	}

	public abstract MethodInfo createMethodInfo();

	public abstract void makeMethodCallable();

	public ExpressionParser createChildParser() {
		ExpressionParser parser = new InnerMethodExpressionParser(this.parser, this.returnType);
		for (UserParameter parameter : this.userParameters.parameters()) {
			parser.environment.user().reserveAndAssignVariable(parameter.name(), parameter.type());
		}
		return parser;
	}

	public void parseMethodBody() throws ScriptParsingException {
		this.parser.environment.user().push();
		ExpressionParser newParser = this.createChildParser();
		InsnTree body = newParser.nextScript();
		if (!newParser.input.hasAfterWhitespace(')')) {
			throw new ScriptParsingException("Unexpected trailing character: " + newParser.input.peekAfterWhitespace(), newParser.input);
		}
		if (!body.jumpsUnconditionally()) body = newParser.createReturn(body);
		this.parser.environment.user().pop();

		this.emitMethodBytecode(body);
	}

	public abstract void emitMethodBytecode(InsnTree body);

	public void emitMethodBytecode(Stream<LazyVarInfo> parameters, InsnTree body) {
		MethodCompileContext newMethod = this.parser.clazz.newMethod(this.newMethod.access(), this.newMethod.name, this.newMethod.returnType, parameters.toArray(LazyVarInfo.ARRAY_FACTORY));
		body.emitBytecode(newMethod);
		newMethod.endCode();
	}

	public static class UserFunctionDefiner extends UserMethodDefiner {

		public UserFunctionDefiner(ExpressionParser parser, String methodName, TypeInfo returnType) {
			super(parser, methodName, returnType);
		}

		@Override
		public MethodInfo createMethodInfo() {
			return this.createMethodInfo(
				Stream.concat(
					this.streamUserParameterTypes(),
					this.streamImplicitParameterTypes()
				)
			);
		}

		@Override
		public void makeMethodCallable() {
			MethodInfo method = this.newMethod;
			TypeInfo callerInfo = this.parser.clazz.info;
			this.parser.environment.user().addFunction(
				this.methodName,
				method.isStatic()
				? (ExpressionParser parser, String name, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, Arrays.stream(this.userParameters.parameters()).map(UserParameter::type).toArray(TypeInfo.ARRAY_FACTORY), CastMode.IMPLICIT_THROW, arguments);
					InsnTree[] concatArguments = ObjectArrays.concat(castArguments, this.implicitParameters.toArray(LoadInsnTree[]::new), InsnTree.class);
					return new CastResult(invokeStatic(method, concatArguments), castArguments != arguments);
				}
				: (ExpressionParser parser, String name, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, Arrays.stream(this.userParameters.parameters()).map(UserParameter::type).toArray(TypeInfo.ARRAY_FACTORY), CastMode.IMPLICIT_THROW, arguments);
					InsnTree[] concatArguments = ObjectArrays.concat(castArguments, this.implicitParameters.toArray(LoadInsnTree[]::new), InsnTree.class);
					return new CastResult(invokeInstance(load("this", callerInfo), method, concatArguments), castArguments != arguments);
				}
			);
		}

		@Override
		public void emitMethodBytecode(InsnTree body) {
			this.emitMethodBytecode(
				Stream.concat(
					this.streamUserParameters(),
					this.streamImplicitParameters()
				),
				body
			);
		}
	}

	public static class UserExtensionMethodDefiner extends UserMethodDefiner {

		public final TypeInfo typeBeingExtended;

		public UserExtensionMethodDefiner(ExpressionParser parser, String methodName, TypeInfo returnType, TypeInfo typeBeingExtended) {
			super(parser, methodName, returnType);
			this.typeBeingExtended = typeBeingExtended;
		}

		@Override
		public ExpressionParser createChildParser() {
			ExpressionParser newParser = super.createChildParser();
			newParser.environment.user().reserveAndAssignVariable("this", this.typeBeingExtended);
			return newParser;
		}

		@Override
		public MethodInfo createMethodInfo() {
			return this.createMethodInfo(
				Stream.of(
					Stream.of(this.typeBeingExtended),
					this.streamUserParameterTypes(),
					this.streamImplicitParameterTypes()
				)
				.flatMap(Function.identity())
			);
		}

		@Override
		public void makeMethodCallable() {
			MethodInfo method = this.newMethod;
			TypeInfo callerInfo = this.parser.clazz.info;
			this.parser.environment.user().addMethod(
				this.typeBeingExtended,
				this.methodName,
				method.isStatic()
				? (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, Arrays.stream(this.userParameters.parameters()).map(UserParameter::type).toArray(TypeInfo.ARRAY_FACTORY), CastMode.IMPLICIT_THROW, arguments);
					InsnTree[] concatArguments = concat(receiver, castArguments, this.implicitParameters.toArray(LoadInsnTree[]::new));
					return new CastResult(mode.makeInvoker(parser, method, concatArguments), castArguments != arguments);
				}
				: (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, Arrays.stream(this.userParameters.parameters()).map(UserParameter::type).toArray(TypeInfo.ARRAY_FACTORY), CastMode.IMPLICIT_THROW, arguments);
					InsnTree[] concatArguments = concat(receiver, castArguments, this.implicitParameters.toArray(LoadInsnTree[]::new));
					return new CastResult(mode.makeInvoker(parser, load("this", callerInfo), method, concatArguments), castArguments != arguments);
				}
			);
		}

		@Override
		public void emitMethodBytecode(InsnTree body) {
			this.emitMethodBytecode(
				Stream.of(
					Stream.of(new LazyVarInfo("this", this.typeBeingExtended)),
					this.streamUserParameters(),
					this.streamImplicitParameters()
				)
				.flatMap(Function.identity()),
				body
			);
		}

		public static InsnTree[] concat(InsnTree receiver, InsnTree[] userParameters, InsnTree[] implicitParameters) {
			InsnTree[] result = new InsnTree[1 + userParameters.length + implicitParameters.length];
			result[0] = receiver;
			System.arraycopy(userParameters, 0, result, 1, userParameters.length);
			System.arraycopy(implicitParameters, 0, result, userParameters.length + 1, implicitParameters.length);
			return result;
		}
	}
}