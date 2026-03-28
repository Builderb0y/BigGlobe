package builderb0y.scripting.parsing.special;

import java.util.ArrayList;
import java.util.List;

import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record CommaSeparatedExpressions(InsnTree[] arguments, boolean hasNewVariables) implements CodeBlock {

	public static CommaSeparatedExpressions parse(ExpressionParser parser) throws ScriptParsingException {
		parser.beginCodeBlock();
		if (parser.input.peekAfterWhitespace() == ')') {
			parser.endCodeBlock();
			return new CommaSeparatedExpressions(InsnTree.ARRAY_FACTORY.empty(), false);
		}
		List<InsnTree> args = new ArrayList<>(4);
		boolean newVariables;
		while (true) {
			args.add(parser.nextScript());
			if (parser.input.hasAfterWhitespace(',')) {
				continue;
			}
			else {
				newVariables = parser.endCodeBlock();
				break;
			}
		}
		return new CommaSeparatedExpressions(InsnTree.ARRAY_FACTORY.collectionToArray(args), newVariables);
	}
}