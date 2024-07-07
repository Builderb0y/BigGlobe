package builderb0y.scripting.parsing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;

import builderb0y.bigglobe.chunkgen.scripted.SurfaceScript.AnyNumericTypeExpressionParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.DelayedMethod.LazyInvokeInsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.ConditionalNegateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ReturnInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList.UserParameter;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class UserMethodDefiner extends VariableCapturer {

	public final String methodName;
	public final TypeInfo returnType;

	public UserParameterList userParameters;
	public DelayedMethod newMethod;

	public UserMethodDefiner(ExpressionParser parser, String methodName, TypeInfo returnType) {
		super(parser);
		this.methodName = methodName;
		this.returnType = returnType;
	}

	public void parse() throws ScriptParsingException {
		this.parseUserParameters();
		this.addBuiltinParameters();
		this.addCapturedParameters();
		this.parser.delayedMethods.add(this.newMethod = new DelayedMethod(this));
		this.makeMethodCallable();
		this.parseMethodBody();
	}

	public void parseUserParameters() throws ScriptParsingException {
		this.userParameters = UserParameterList.parse(this.parser);
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

	public abstract void makeMethodCallable();

	public ExpressionParser createChildParser() {
		return new InnerMethodExpressionParser(this.parser, this.returnType);
	}

	public void parseMethodBody() throws ScriptParsingException {
		this.parser.environment.user().push();
		ExpressionParser newParser = this.createChildParser();
		this.newMethod.configureEnvironment(newParser);
		InsnTree body = newParser.nextScript();
		if (!newParser.input.hasAfterWhitespace(')')) {
			throw new ScriptParsingException("Unexpected trailing character: " + newParser.input.peekAfterWhitespace(), newParser.input);
		}
		if (!body.jumpsUnconditionally()) body = newParser.createReturn(body);
		this.parser.environment.user().pop();

		this.newMethod.body = body;
	}

	public static class UserFunctionDefiner extends UserMethodDefiner {

		public UserFunctionDefiner(ExpressionParser parser, String methodName, TypeInfo returnType) {
			super(parser, methodName, returnType);
		}

		@Override
		public void makeMethodCallable() {
			DelayedMethod method = this.newMethod;
			TypeInfo callerInfo = this.parser.clazz.info;
			this.parser.environment.user().addFunction(
				this.methodName,
				this.parser.method.info.isStatic()
				? (ExpressionParser parser, String name, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, Arrays.stream(this.userParameters.parameters()).map(UserParameter::type).toArray(TypeInfo.ARRAY_FACTORY), CastMode.IMPLICIT_THROW, arguments);
					if (method.body != null) method.streamCapturedArgs().forEach(parser.environment.user()::markVariableUsed);
					return new CastResult(
						new LazyInvokeInsnTree(
							() -> {
								InsnTree[] concatArguments = ObjectArrays.concat(castArguments, method.streamCapturedArgs().map(InsnTrees::load).toArray(InsnTree.ARRAY_FACTORY), InsnTree.class);
								return invokeStatic(method.methodInfo, concatArguments);
							},
							method.returnType
						),
						castArguments != arguments
					);
				}
				: (ExpressionParser parser, String name, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, Arrays.stream(this.userParameters.parameters()).map(UserParameter::type).toArray(TypeInfo.ARRAY_FACTORY), CastMode.IMPLICIT_THROW, arguments);
					if (method.body != null) method.streamCapturedArgs().forEach(parser.environment.user()::markVariableUsed);
					return new CastResult(
						new LazyInvokeInsnTree(
							() -> {
								InsnTree[] concatArguments = ObjectArrays.concat(castArguments, method.streamCapturedArgs().map(InsnTrees::load).toArray(InsnTree.ARRAY_FACTORY), InsnTree.class);
								return invokeInstance(load("this", callerInfo), method.methodInfo, concatArguments);
							},
							method.returnType
						),
						castArguments != arguments
					);
				}
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
		public Stream<LazyVarInfo> streamUserParameters() {
			return Stream.concat(Stream.of(new LazyVarInfo("this", this.typeBeingExtended)), super.streamUserParameters());
		}

		@Override
		public void makeMethodCallable() {
			DelayedMethod method = this.newMethod;
			TypeInfo callerInfo = this.parser.clazz.info;
			TypeInfo typeBeingExtended = this.typeBeingExtended;
			this.parser.environment.user().addMethod(
				typeBeingExtended,
				this.methodName,
				this.parser.method.info.isStatic()
				? (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, Arrays.stream(this.userParameters.parameters()).map(UserParameter::type).toArray(TypeInfo.ARRAY_FACTORY), CastMode.IMPLICIT_THROW, arguments);
					if (method.body != null) method.streamCapturedArgs().forEach(parser.environment.user()::markVariableUsed);
					return new CastResult(
						new LazyInvokeInsnTree(
							() -> {
								InsnTree[] concatArguments = concat(receiver, castArguments, method.streamCapturedArgs().map(InsnTrees::load).toArray(InsnTree.ARRAY_FACTORY));
								return mode.makeInvoker(parser, method.methodInfo, concatArguments);
							},
							switch (mode) {
								case NORMAL, NULLABLE -> method.returnType;
								case RECEIVER, NULLABLE_RECEIVER -> typeBeingExtended;
							}
						),
						castArguments != arguments
					);
				}
				: (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, Arrays.stream(this.userParameters.parameters()).map(UserParameter::type).toArray(TypeInfo.ARRAY_FACTORY), CastMode.IMPLICIT_THROW, arguments);
					if (method.body != null) method.streamCapturedArgs().forEach(parser.environment.user()::markVariableUsed);
					return new CastResult(
						new LazyInvokeInsnTree(
							() -> {
								InsnTree[] concatArguments = concat(receiver, castArguments, method.streamCapturedArgs().map(InsnTrees::load).toArray(InsnTree.ARRAY_FACTORY));
								return mode.makeInvoker(parser, load("this", callerInfo), method.methodInfo, concatArguments);
							},
							switch (mode) {
								case NORMAL, NULLABLE -> method.returnType;
								case RECEIVER, NULLABLE_RECEIVER -> typeBeingExtended;
							}
						),
						castArguments != arguments
					);
				}
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

	public static class DerivativeMethodDefiner extends UserMethodDefiner {

		public DerivativeMethodDefiner(ExpressionParser parser, String methodName) {
			super(parser, methodName, null);
		}

		public InsnTree createDerivative(TypeInfo columnType, boolean z) throws ScriptParsingException {
			boolean wasEmpty = this.parser.delayedMethods.isEmpty();
			this.parse();
			if (wasEmpty) this.parser.finishDelayedMethods();
			LazyVarInfo
				mainColumn       = new LazyVarInfo("mainColumn",       columnType),
				adjacentColumnX  = new LazyVarInfo("adjacentColumnX",  columnType),
				adjacentColumnZ  = new LazyVarInfo("adjacentColumnZ",  columnType),
				adjacentColumnXZ = new LazyVarInfo("adjacentColumnXZ", columnType);

			InsnTree normalInvoker, adjacentInvoker;
			InsnTree[] normalArgs = this.newMethod.streamCapturedArgs().map(InsnTrees::load).toArray(InsnTree.ARRAY_FACTORY);
			InsnTree[] adjacentArgs = this.newMethod.streamCapturedArgs().map((LazyVarInfo variable) -> {
				return switch (variable.name) {
					case "mainColumn"       -> z ? adjacentColumnZ  : adjacentColumnX ;
					case "adjacentColumnX"  -> z ? adjacentColumnXZ : mainColumn      ;
					case "adjacentColumnZ"  -> z ? mainColumn       : adjacentColumnXZ;
					case "adjacentColumnXZ" -> z ? adjacentColumnX  : adjacentColumnZ ;
					default -> variable;
				};
			})
			.map(InsnTrees::load)
			.toArray(InsnTree.ARRAY_FACTORY);
			if (this.newMethod.methodInfo.isStatic()) {
				normalInvoker   = invokeStatic(this.newMethod.methodInfo, normalArgs);
				adjacentInvoker = invokeStatic(this.newMethod.methodInfo, adjacentArgs);
			}
			else {
				normalInvoker   = invokeInstance(load("this", this.parser.clazz.info), this.newMethod.methodInfo, normalArgs);
				adjacentInvoker = invokeInstance(load("this", this.parser.clazz.info), this.newMethod.methodInfo, adjacentArgs);
			}
			return ConditionalNegateInsnTree.create(
				this.parser,
				sub(this.parser, adjacentInvoker, normalInvoker),
				lt(
					this.parser,
					z ? ScriptedColumn.INFO.z(load(adjacentColumnZ)) : ScriptedColumn.INFO.x(load(adjacentColumnX)),
					z ? ScriptedColumn.INFO.z(load(mainColumn     )) : ScriptedColumn.INFO.x(load(mainColumn     ))
				)
			);
		}

		@Override
		public void parseUserParameters() throws ScriptParsingException {
			this.userParameters = new UserParameterList();
		}

		@Override
		public void makeMethodCallable() {
			//no-op.
		}

		@Override
		public ExpressionParser createChildParser() {
			AnyNumericTypeExpressionParser newParser = new AnyNumericTypeExpressionParser(this.parser);
			newParser.environment.mutable().functions.put("return", Collections.singletonList((ExpressionParser parser1, String name1, InsnTree... arguments) -> {
				throw new ScriptParsingException("For technical reasons, you cannot return from inside a derivative block", parser1.input);
			}));
			List<FunctionHandler> higherOrderDerivatives = Collections.singletonList((ExpressionParser parser1, String name1, InsnTree... arguments) -> {
				throw new ScriptParsingException("Higher order derivatives are not supported.", parser1.input);
			});
			newParser.environment.mutable().functions.put("dx", higherOrderDerivatives);
			newParser.environment.mutable().functions.put("dz", higherOrderDerivatives);
			return newParser;
		}

		@Override
		public void parseMethodBody() throws ScriptParsingException {
			super.parseMethodBody();
			this.newMethod.returnType = ((ReturnInsnTree)(this.newMethod.body)).value.getTypeInfo();
		}
	}
}