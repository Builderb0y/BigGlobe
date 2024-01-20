package builderb0y.scripting.bytecode.loops;

import java.util.List;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.AbstractForIteratorInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForMapIteratorInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MapLoopFactory implements LoopFactory {

	public InsnTree map;

	public MapLoopFactory(InsnTree map) {
		this.map = map;
	}

	@Override
	public InsnTree createLoop(ExpressionParser parser, LoopName loopName, List<VariableDeclarationInsnTree> variables, InsnTree body) throws ScriptParsingException {
		if (variables.size() != 2) {
			throw new ScriptParsingException("Iteration over map requires 2 variables, for keys and values", parser.input);
		}
		return new ForMapIteratorInsnTree(
			loopName,
			variables.get(0),
			variables.get(1),
			new VariableDeclareAssignInsnTree(
				new LazyVarInfo(
					parser.method.mangleName("iterator"),
					TypeInfos.ITERATOR
				),
				invokeInstance(
					invokeInstance(this.map, ForMapIteratorInsnTree.ENTRY_SET),
					AbstractForIteratorInsnTree.ITERATOR
				)
			),
			body
		);
	}
}