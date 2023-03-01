package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public abstract class UpdateInsnTree implements InsnTree {

	public InsnTree updater;

	public UpdateInsnTree(InsnTree updater) {
		this.updater = updater;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}