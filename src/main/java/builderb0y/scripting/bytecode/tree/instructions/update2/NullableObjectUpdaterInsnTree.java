package builderb0y.scripting.bytecode.tree.instructions.update2;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableObjectUpdaterInsnTree extends AbstractObjectUpdaterInsnTree {

	public NullableObjectUpdaterInsnTree(CombinedMode mode, ObjectUpdaterEmitters emitters) {
		super(mode, emitters);
	}

	public NullableObjectUpdaterInsnTree(UpdateOrder order, boolean isAssignment, ObjectUpdaterEmitters emitters) {
		super(order, isAssignment, emitters);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label update = label(), end = label();

		this.emitObject(method); //object
		method.node.visitInsn(DUP); //object object
		method.node.visitJumpInsn(IFNONNULL, update); //object
		method.node.visitInsn(POP); //
		if (this.mode.order != UpdateOrder.VOID) {
			constantAbsent(this.getTypeInfo()).emitBytecode(method);
		}
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(update); //object
		switch (this.mode) {
			case VOID -> {
				method.node.visitInsn(DUP);
				this.emitGet(method);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case PRE -> {
				method.node.visitInsn(DUP);
				this.emitGet(method);
				method.node.visitInsn(this.getTypeInfo().isDoubleWidth() ? DUP2_X1 : DUP_X1);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case POST -> {
				method.node.visitInsn(DUP);
				this.emitGet(method);
				this.emitUpdate(method);
				method.node.visitInsn(this.getTypeInfo().isDoubleWidth() ? DUP2_X1 : DUP_X1);
				this.emitSet(method);
			}
			case VOID_ASSIGN -> {
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case PRE_ASSIGN -> {
				method.node.visitInsn(DUP); //object object
				this.emitGet(method); //object value
				if (this.getPreType().isDoubleWidth()) {
					method.node.visitInsn(DUP2_X1);
					method.node.visitInsn(POP2);
				}
				else {
					method.node.visitInsn(SWAP);
				}
				//value object
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case POST_ASSIGN -> {
				this.emitUpdate(method); //object value
				method.node.visitInsn(this.getPostType().isDoubleWidth() ? DUP2_X1 : DUP_X1); //value object value
				this.emitSet(method); //value
			}
		}

		method.node.visitLabel(end);
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new NullableObjectUpdaterInsnTree(this.mode.asVoid(), this.emitters);
	}
}