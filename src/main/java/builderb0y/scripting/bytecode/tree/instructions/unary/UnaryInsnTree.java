package builderb0y.scripting.bytecode.tree.instructions.unary;

import builderb0y.scripting.bytecode.tree.InsnTree;

public abstract class UnaryInsnTree implements InsnTree {

	public InsnTree operand;

	public UnaryInsnTree(InsnTree operand) {
		this.operand = operand;
	}
}