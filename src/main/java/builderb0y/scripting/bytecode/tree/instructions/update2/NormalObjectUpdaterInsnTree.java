package builderb0y.scripting.bytecode.tree.instructions.update2;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class NormalObjectUpdaterInsnTree extends AbstractObjectUpdaterInsnTree {

	public NormalObjectUpdaterInsnTree(CombinedMode mode, ObjectUpdaterEmitters emitters) {
		super(mode, emitters);
	}

	public NormalObjectUpdaterInsnTree(UpdateOrder order, boolean isAssignment, ObjectUpdaterEmitters emitters) {
		super(order, isAssignment, emitters);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		switch (this.mode) {
			case VOID -> {
				this.emitObject(method);
				method.node.visitInsn(DUP);
				this.emitGet(method);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case PRE -> {
				this.emitObject(method);
				method.node.visitInsn(DUP);
				this.emitGet(method);
				method.node.visitInsn(this.getPreType().isDoubleWidth() ? DUP2_X1 : DUP_X1);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case POST -> {
				this.emitObject(method);
				method.node.visitInsn(DUP);
				this.emitGet(method);
				this.emitUpdate(method);
				method.node.visitInsn(this.getPostType().isDoubleWidth() ? DUP2_X1 : DUP_X1);
				this.emitSet(method);
			}
			case VOID_ASSIGN -> {
				this.emitObject(method);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case PRE_ASSIGN -> {
				this.emitObject(method);
				method.node.visitInsn(DUP);
				this.emitGet(method);
				if (this.getPreType().isDoubleWidth()) {
					method.node.visitInsn(DUP2_X1);
					method.node.visitInsn(POP2);
				}
				else {
					method.node.visitInsn(SWAP);
				}
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case POST_ASSIGN -> {
				this.emitObject(method);
				this.emitUpdate(method);
				method.node.visitInsn(this.getPostType().isDoubleWidth() ? DUP2_X1 : DUP_X1);
				this.emitSet(method);
			}
		}
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new NormalObjectUpdaterInsnTree(this.mode.asVoid(), this.emitters);
	}
}