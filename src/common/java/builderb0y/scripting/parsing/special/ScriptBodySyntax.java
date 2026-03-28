package builderb0y.scripting.parsing.special;

import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record ScriptBodySyntax(InsnTree expression, InsnTree body, boolean hasNewVariables) implements CodeBlock {

	public static ScriptBodySyntax parse(ExpressionParser parser, HeaderProcessor headerProcessor) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		parser.environment.user().push();
		InsnTree expression = headerProcessor.apply(parser.nextScript(), parser);
		parser.input.expectOperatorAfterWhitespace(":");
		InsnTree body = parser.nextScript();
		boolean hasNewVariables = parser.environment.user().hasNewVariables();
		parser.environment.user().pop();
		parser.input.expectAfterWhitespace(')');
		return new ScriptBodySyntax(expression, body, hasNewVariables);
	}

	@FunctionalInterface
	public static interface HeaderProcessor {

		public abstract InsnTree apply(InsnTree header, ExpressionParser parser) throws ScriptParsingException;
	}
}