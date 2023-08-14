package builderb0y.scripting.bytecode.tree.instructions.update2;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableReceiverObjectUpdaterInsnTree extends AbstractObjectUpdaterInsnTree {

	public NullableReceiverObjectUpdaterInsnTree(CombinedMode mode, ObjectUpdaterEmitters emitters) {
		super(mode, emitters);
		ReceiverObjectUpdaterInsnTree.checkUsage(mode);
	}

	public NullableReceiverObjectUpdaterInsnTree(UpdateOrder order, boolean isAssignment, ObjectUpdaterEmitters emitters) {
		super(order, isAssignment, emitters);
		ReceiverObjectUpdaterInsnTree.checkUsage(this.mode);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label update = label(), end = label();

		this.emitObject(method); //object
		method.node.visitInsn(DUP); //object object
		method.node.visitInsn(DUP); //object object object
		method.node.visitJumpInsn(IFNONNULL, update); //object object
		method.node.visitInsn(POP); //object
		method.node.visitJumpInsn(GOTO, end); //object

		method.node.visitLabel(update);
		if (!this.mode.isAssignment) {
			method.node.visitInsn(DUP); //object object object
			this.emitGet(method); //object object value
		}
		this.emitUpdate(method); //object object value
		this.emitSet(method); //object

		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.emitters.objectType();
	}

	@Override
	public InsnTree asStatement() {
		return new NullableObjectUpdaterInsnTree(this.mode.asVoid(), this.emitters);
	}
}