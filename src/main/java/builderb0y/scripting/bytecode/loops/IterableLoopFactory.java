package builderb0y.scripting.bytecode.loops;

import java.util.List;

import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.AbstractForIteratorInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForIteratorInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class IterableLoopFactory implements LoopFactory {

	public InsnTree iterable;

	public IterableLoopFactory(InsnTree iterable) {
		this.iterable = iterable;
	}

	@Override
	public InsnTree createLoop(ExpressionParser parser, LoopName loopName, List<VariableDeclarationInsnTree> variables, InsnTree body) throws ScriptParsingException {
		return new ForIteratorInsnTree(
			loopName,
			variables.get(0),
			new VariableDeclareAssignInsnTree(
				"$iterator",
				TypeInfos.ITERATOR,
				invokeInstance(
					this.iterable,
					AbstractForIteratorInsnTree.ITERATOR
				)
			),
			body
		);
	}
}