package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class NewInsnTree extends InvokeStaticInsnTree {

	public NewInsnTree(MethodInfo constructor, InsnTree... args) {
		super(constructor, args);
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

	public static class UnusedNewInsnTree extends InvokeStaticInsnTree {

		public UnusedNewInsnTree(MethodInfo method, InsnTree... args) {
			super(method, args);
			if (method.isStatic() || !method.name.equals("<init>") || !method.returnType.isVoid()) {
				throw new IllegalArgumentException(method.toString());
			}
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			method.node.visitTypeInsn(NEW, this.method.owner.getInternalName());
			super.emitBytecode(method);
		}
	}
}