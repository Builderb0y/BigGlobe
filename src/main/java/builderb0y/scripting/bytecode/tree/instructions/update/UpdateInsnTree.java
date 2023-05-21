package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.tree.InsnTree;

public interface UpdateInsnTree extends InsnTree {

	@Override
	public default boolean canBeStatement() {
		return true;
	}
}