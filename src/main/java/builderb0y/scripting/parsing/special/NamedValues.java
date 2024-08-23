package builderb0y.scripting.parsing.special;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record NamedValues(NamedValue[] values, boolean hasNewVariables) implements CodeBlock {

	public static record NamedValue(String name, InsnTree value) {}

	public static NamedValues parse(ExpressionParser parser, @Nullable TypeInfo valueType, @Nullable NameChecker nameChecker) throws ScriptParsingException {
		parser.beginCodeBlock();
		List<NamedValue> namedValues = new ArrayList<>(8);
		if (parser.input.peekAfterWhitespace() != ')') {
			while (true) {
				String name = parser.input.expectIdentifierAfterWhitespace();
				if (nameChecker != null) nameChecker.checkName(parser, name);
				parser.input.expectOperatorAfterWhitespace(":");
				InsnTree value = parser.nextScript();
				if (valueType != null) {
					value = value.cast(parser, valueType, CastMode.IMPLICIT_THROW);
				}
				namedValues.add(new NamedValue(name, value));
				if (parser.input.hasOperatorAfterWhitespace(",")) continue;
				else if (parser.input.peekAfterWhitespace() == ')') break;
				else throw new ScriptParsingException("Expected ',' or ')'", parser.input);
			}
		}
		boolean hasNewVariables = parser.endCodeBlock();
		return new NamedValues(namedValues.toArray(new NamedValue[namedValues.size()]), hasNewVariables);
	}

	@FunctionalInterface
	public static interface NameChecker {

		public abstract void checkName(ExpressionParser parser, String name) throws ScriptParsingException;
	}
}