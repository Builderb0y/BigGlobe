package builderb0y.scripting.parsing.special;

import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record ConditionBodySyntax(ConditionTree condition, InsnTree body, boolean hasNewVariables) implements CodeBlock {

	public static ConditionBodySyntax parse(ExpressionParser parser) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		parser.environment.user().push();
		ConditionTree condition = InsnTrees.condition(parser, parser.nextScript());
		parser.input.expectOperatorAfterWhitespace(":");
		InsnTree body = parser.nextScript();
		boolean hasNewVariables = parser.environment.user().hasNewVariables();
		parser.environment.user().pop();
		parser.input.expectAfterWhitespace(')');
		return new ConditionBodySyntax(condition, body, hasNewVariables);
	}
}