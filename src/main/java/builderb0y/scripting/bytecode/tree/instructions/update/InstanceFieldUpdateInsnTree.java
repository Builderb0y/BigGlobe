package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class InstanceFieldUpdateInsnTree extends UpdateInsnTree {

	public InsnTree object;
	public FieldInfo field;

	public InstanceFieldUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
		super(updater);
		this.object  = object;
		this.field   = field;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.object.emitBytecode(method);
		method.node.visitInsn(DUP);
		this.field.emitGet(method);
		this.updater.emitBytecode(method);
		this.field.emitPut(method);
	}
}