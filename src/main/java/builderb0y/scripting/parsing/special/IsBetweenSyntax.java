package builderb0y.scripting.parsing.special;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.between.BetweenInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

public record IsBetweenSyntax(InsnTree value, InsnTree min, boolean minInclusive, InsnTree max, boolean maxInclusive) {

	public static IsBetweenSyntax parse(ExpressionParser parser, InsnTree receiver) throws ScriptParsingException {
		TypeInfo expectedType = TypeInfos.widenToInt(receiver.getTypeInfo());
		if (!expectedType.isNumber()) {
			throw new ScriptParsingException("Value must be numeric", parser.input);
		}
		boolean minInclusive = switch (parser.input.readAfterWhitespace()) {
			case '[' -> true;
			case '(' -> false;
			default -> throw new ScriptParsingException("Expected '[' or '('", parser.input);
		};
		parser.environment.user().push();
		InsnTree min = parser.nextScript().cast(parser, expectedType, CastMode.IMPLICIT_THROW);
		parser.input.expectOperatorAfterWhitespace(",");
		InsnTree max = parser.nextScript().cast(parser, expectedType, CastMode.IMPLICIT_THROW);
		boolean maxInclusive = switch (parser.input.readAfterWhitespace()) {
			case ']' -> true;
			case ')' -> false;
			default -> throw new ScriptParsingException("Expected ']' or ')'", parser.input);
		};
		parser.environment.user().pop();
		return new IsBetweenSyntax(receiver, min, minInclusive, max, maxInclusive);
	}

	public InsnTree toTree(ExpressionParser parser) {
		return BetweenInsnTree.create(parser, this.value, this.min, this.minInclusive, this.max, this.maxInclusive);
	}
}