package builderb0y.scripting.bytecode.tree.instructions.fields;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.AbstractObjectUpdaterInsnTree.ObjectUpdaterEmitters;
import builderb0y.scripting.bytecode.tree.instructions.update2.NullableReceiverObjectUpdaterInsnTree;

public class NullableReceiverInstanceGetFieldInsnTree extends AbstractInstanceGetFieldInsnTree {

	public NullableReceiverInstanceGetFieldInsnTree(InsnTree object, FieldInfo field) {
		super(object, field);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.object.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.object.getTypeInfo();
	}

	@Override
	public InsnTree constructUpdater(UpdateOrder order, boolean isAssignment, ObjectUpdaterEmitters emitters) {
		return new NullableReceiverObjectUpdaterInsnTree(order, isAssignment, emitters);
	}
}