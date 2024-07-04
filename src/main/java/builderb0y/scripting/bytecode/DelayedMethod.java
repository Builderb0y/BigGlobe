package builderb0y.scripting.bytecode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.UserMethodDefiner;

public class DelayedMethod {

	public String methodName;
	public TypeInfo returnType;
	public Map<LazyVarInfo, Boolean> capturedVariables;
	public List<LazyVarInfo> userParameters;
	public @Nullable InsnTree body;
	public @Nullable MethodInfo methodInfo;

	public DelayedMethod(UserMethodDefiner spec) {
		this.methodName = spec.methodName;
		this.returnType = spec.returnType;
		this.capturedVariables = new HashMap<>(spec.builtinParameters.size() + spec.capturedVariables.size());
		for (LoadInsnTree builtin : spec.builtinParameters) {
			this.capturedVariables.put(builtin.variable, Boolean.TRUE);
		}
		for (LoadInsnTree captured : spec.capturedVariables) {
			this.capturedVariables.put(captured.variable, Boolean.FALSE);
		}
		this.userParameters = spec.streamUserParameters().toList();
	}

	public void configureEnvironment(ExpressionParser parser) {
		parser.environment.user().delayedMethods.add(this);
		for (LazyVarInfo variable : this.userParameters) {
			parser.environment.user().reserveAndAssignVariable(variable.name, variable.type);
		}
	}

	public void onVariableUsed(LazyVarInfo info) {
		this.capturedVariables.replace(info, Boolean.TRUE);
	}

	public Stream<LazyVarInfo> streamCapturedArgs() {
		if (this.body == null) throw new IllegalStateException("body not parsed yet!");
		return this.capturedVariables.entrySet().stream().filter(Map.Entry<LazyVarInfo, Boolean>::getValue).map(Map.Entry<LazyVarInfo, Boolean>::getKey);
	}

	public MethodCompileContext createMethod(ExpressionParser parser) {
		if (this.body == null) throw new IllegalStateException("body not parsed yet!");
		MethodCompileContext method = parser.clazz.newMethod(
			parser.method.info.access(),
			this.methodName + '_' + parser.clazz.memberUniquifier++,
			this.returnType,
			Stream.concat(
				this.userParameters.stream(),
				this.streamCapturedArgs()
			)
			.toArray(LazyVarInfo.ARRAY_FACTORY)
		);
		this.methodInfo = method.info;
		return method;
	}

	public void emitMethod(MethodCompileContext method) {
		this.body.emitBytecode(method);
		method.endCode();
	}

	public static class LazyInvokeInsnTree implements InsnTree {

		public Supplier<? extends InsnTree> delegate;
		public TypeInfo type;

		public LazyInvokeInsnTree(Supplier<? extends InsnTree> delegate, TypeInfo type) {
			this.delegate = delegate;
			this.type = type;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.delegate.get().emitBytecode(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public boolean canBeStatement() {
			return true;
		}
	}
}