package builderb0y.scripting.parsing.special;

import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record IntervalSyntax(InsnTree min, boolean minInclusive, InsnTree max, boolean maxInclusive) {

	public static IntervalSyntax parse(ExpressionParser parser) throws ScriptParsingException {
		boolean minInclusive = switch (parser.input.readAfterWhitespace()) {
			case '[' -> true;
			case '(' -> false;
			default -> throw new ScriptParsingException("Expected '[' or '('", parser.input);
		};
		parser.environment.user().push();
		InsnTree min = parser.nextScript();
		parser.input.expectOperatorAfterWhitespace(",");
		InsnTree max = parser.nextScript();
		boolean maxInclusive = switch (parser.input.readAfterWhitespace()) {
			case ']' -> true;
			case ')' -> false;
			default -> throw new ScriptParsingException("Expected ']' or ')'", parser.input);
		};
		parser.environment.user().pop();
		return new IntervalSyntax(min, minInclusive, max, maxInclusive);
	}
}