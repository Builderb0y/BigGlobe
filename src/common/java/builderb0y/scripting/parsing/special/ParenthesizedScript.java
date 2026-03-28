package builderb0y.scripting.parsing.special;

import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record ParenthesizedScript(InsnTree contents, boolean hasNewVariables) implements CodeBlock {

	public static ParenthesizedScript parse(ExpressionParser parser) throws ScriptParsingException {
		parser.beginCodeBlock();
		InsnTree result = parser.nextScript();
		boolean newVariables = parser.endCodeBlock();
		return new ParenthesizedScript(result, newVariables);
	}

	public InsnTree maybeWrapContents() {
		return this.maybeWrap(this.contents);
	}
}