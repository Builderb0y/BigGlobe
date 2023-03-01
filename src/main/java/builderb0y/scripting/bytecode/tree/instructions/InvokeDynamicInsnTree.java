package builderb0y.scripting.bytecode.tree.instructions;

import java.util.Arrays;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class InvokeDynamicInsnTree implements InsnTree {

	public MethodInfo bootstrapMethod, runtimeMethod;
	public ConstantValue[] bootstrapArgs;
	public InsnTree[] runtimeArgs;

	public InvokeDynamicInsnTree(
		MethodInfo bootstrapMethod,
		MethodInfo runtimeMethod,
		ConstantValue[] bootstrapArgs,
		InsnTree[] runtimeArgs
	) {
		this.bootstrapMethod = bootstrapMethod;
		this.runtimeMethod   = runtimeMethod;
		this.bootstrapArgs   = bootstrapArgs;
		this.runtimeArgs     = runtimeArgs;
	}

	public static InvokeDynamicInsnTree create(
		MethodInfo bootstrapMethod,
		MethodInfo runtimeMethod,
		ConstantValue[] bootstrapArgs,
		InsnTree[] runtimeArgs
	) {
		if (!bootstrapMethod.isStatic()) throw new IllegalArgumentException("Non-static bootstrap method: " + bootstrapMethod);
		return new InvokeDynamicInsnTree(bootstrapMethod, runtimeMethod, bootstrapArgs, runtimeArgs);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		for (InsnTree arg : this.runtimeArgs) {
			arg.emitBytecode(method);
		}
		method.node.visitInvokeDynamicInsn(
			this.runtimeMethod.name,
			this.runtimeMethod.getDescriptor(),
			this.bootstrapMethod.toHandle(H_INVOKESTATIC),
			Arrays.stream(this.bootstrapArgs).map(ConstantValue::asAsmObject).toArray()
		);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.runtimeMethod.returnType;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}