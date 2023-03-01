package builderb0y.scripting.bytecode.tree.instructions.unary;

import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

public abstract class UnaryInsnTree implements InsnTree {

	public InsnTree operand;

	public UnaryInsnTree(InsnTree operand) {
		this.operand = operand;
	}

	@Override
	public InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		return this.operand.then(parser, nextStatement);
	}
}