package builderb0y.scripting.bytecode.loops;

import java.util.List;

import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.AbstractForIteratorInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForIteratorInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForIndexedSequentialListInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SequentialListLoopFactory implements LoopFactory {

	public InsnTree list;

	public SequentialListLoopFactory(InsnTree list) {
		this.list = list;
	}

	@Override
	public InsnTree createLoop(ExpressionParser parser, LoopName loopName, List<VariableDeclarationInsnTree> variables, InsnTree body) throws ScriptParsingException {
		return switch (variables.size()) {
			case 1 -> new ForIteratorInsnTree(
				loopName,
				variables.get(0),
				new VariableDeclareAssignInsnTree(
					"$iterator",
					TypeInfos.ITERATOR,
					invokeInstance(this.list, AbstractForIteratorInsnTree.LIST_ITERATOR)
				),
				body
			);
			case 2 -> {
				if (!variables.get(0).variable.type.equals(TypeInfos.INT)) {
					throw new ScriptParsingException("index-value iteration over List requires first variable to be of type int.", parser.input);
				}
				yield new ForIndexedSequentialListInsnTree(
					loopName,
					variables.get(0),
					variables.get(1),
					new VariableDeclareAssignInsnTree(
						"$iterator",
						TypeInfos.ITERATOR,
						invokeInstance(this.list, AbstractForIteratorInsnTree.LIST_ITERATOR)
					),
					body
				);
			}
			default -> {
				throw new ScriptParsingException("Iteration over List requires 1 or 2 variables", parser.input);
			}
		};
	}
}