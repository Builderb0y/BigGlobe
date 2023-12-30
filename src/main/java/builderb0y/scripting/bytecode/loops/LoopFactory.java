package builderb0y.scripting.bytecode.loops;

import java.util.List;

import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public interface LoopFactory {

	public abstract InsnTree createLoop(
		ExpressionParser parser,
		LoopName loopName,
		List<VariableDeclarationInsnTree> variables,
		InsnTree body
	)
	throws ScriptParsingException;
}