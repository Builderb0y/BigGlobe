package builderb0y.scripting.bytecode.loops;

import java.util.List;

import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForIndexedRandomAccessListInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForRandomAccessListInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

public class RandomAccessListLoopFactory implements LoopFactory {

	public InsnTree list;

	public RandomAccessListLoopFactory(InsnTree list) {
		this.list = list;
	}

	@Override
	public InsnTree createLoop(ExpressionParser parser, LoopName loopName, List<VariableDeclarationInsnTree> variables, InsnTree body) throws ScriptParsingException {
		return switch (variables.size()) {
			case 1 -> new ForRandomAccessListInsnTree(
				loopName,
				variables.get(0),
				new VariableDeclareAssignInsnTree(
					"$listForIteration",
					this.list.getTypeInfo(),
					this.list
				),
				body
			);
			case 2 -> {
				if (!variables.get(0).variable.type.equals(TypeInfos.INT)) {
					throw new ScriptParsingException("index-value iteration over List requires first variable to be of type int.", parser.input);
				}
				yield new ForIndexedRandomAccessListInsnTree(
					loopName,
					variables.get(0),
					variables.get(1),
					new VariableDeclareAssignInsnTree(
						"$listForIteration",
						this.list.getTypeInfo(),
						this.list
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