package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class NewInsnTree extends BaseInvokeInsnTree {

	public NewInsnTree(MethodInfo constructor, InsnTree... args) {
		super(constructor, args);
		checkConstructor(constructor);
		checkArguments(constructor.paramTypes, args);
	}

	public static void checkConstructor(MethodInfo constructor) {
		if (constructor.isStatic() || !constructor.name.equals("<init>") || !constructor.returnType.isVoid()) {
			throw new IllegalArgumentException(constructor.toString());
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.node.visitTypeInsn(NEW, this.method.owner.getInternalName());
		method.node.visitInsn(DUP);
		super.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.method.owner;
	}

	@Override
	public InsnTree asStatement() {
		return new UnusedNewInsnTree(this.method, this.args);
	}

	public static class UnusedNewInsnTree extends BaseInvokeInsnTree {

		public UnusedNewInsnTree(MethodInfo constructor, InsnTree... args) {
			super(constructor, args);
			checkConstructor(constructor);
			checkArguments(constructor.paramTypes, args);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			method.node.visitTypeInsn(NEW, this.method.owner.getInternalName());
			super.emitBytecode(method);
		}
	}
}