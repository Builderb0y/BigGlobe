package builderb0y.scripting.bytecode.loops;

import java.util.List;

import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForIteratorInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

public class IteratorLoopFactory implements LoopFactory {

	public InsnTree iterator;

	public IteratorLoopFactory(InsnTree iterator) {
		this.iterator = iterator;
	}

	@Override
	public InsnTree createLoop(ExpressionParser parser, LoopName loopName, List<VariableDeclarationInsnTree> variables, InsnTree body) throws ScriptParsingException {
		return new ForIteratorInsnTree(
			loopName,
			variables.get(0),
			new VariableDeclareAssignInsnTree(
				"iterator",
				TypeInfos.ITERATOR,
				this.iterator
			),
			body
		);
	}
}