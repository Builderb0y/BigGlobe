package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class StaticFieldUpdateInsnTree extends UpdateInsnTree {

	public FieldInfo field;

	public StaticFieldUpdateInsnTree(FieldInfo field, InsnTree updater) {
		super(updater);
		this.field = field;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.field.emitGet(method);
		this.updater.emitBytecode(method);
		this.field.emitPut(method);
	}
}