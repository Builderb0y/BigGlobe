package builderb0y.scripting.bytecode.tree.instructions.update2;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ReceiverObjectUpdaterInsnTree extends AbstractObjectUpdaterInsnTree {

	public ReceiverObjectUpdaterInsnTree(CombinedMode mode, ObjectUpdaterEmitters emitters) {
		super(mode, emitters);
		checkUsage(mode);
	}

	public ReceiverObjectUpdaterInsnTree(UpdateOrder order, boolean isAssignment, ObjectUpdaterEmitters emitters) {
		super(order, isAssignment, emitters);
		checkUsage(this.mode);
	}

	public static void checkUsage(CombinedMode mode) {
		switch (mode.order) {
			case VOID -> {}
			case PRE  -> throw new IllegalArgumentException("Can't return receiver and pre at the same time.");
			case POST -> throw new IllegalArgumentException("Can't return receiver and post at the same time.");
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.emitObject(method); //object
		method.node.visitInsn(DUP); //object object
		if (!this.mode.isAssignment) {
			method.node.visitInsn(DUP); //object object object
			this.emitGet(method); //object object value
		}
		this.emitUpdate(method); //object object value
		this.emitSet(method); //object
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.emitters.objectType();
	}

	@Override
	public InsnTree asStatement() {
		return new NormalObjectUpdaterInsnTree(this.mode.asVoid(), this.emitters);
	}
}