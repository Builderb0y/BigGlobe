package builderb0y.scripting.parsing.special;

import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface CodeBlock {

	public abstract boolean hasNewVariables();

	public default InsnTree maybeWrap(InsnTree tree) {
		return this.hasNewVariables() ? scoped(tree) : tree;
	}
}