package builderb0y.scripting.parsing.special;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionReader.CursorPos;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.special.NamedValues.NamedValue;

public record PrefixedNamedValues(@Nullable InsnTree prefix, NamedValue[] values, boolean hasNewVariables) implements CodeBlock {

	public static PrefixedNamedValues parse(ExpressionParser parser, @Nullable TypeInfo prefixType, @Nullable TypeInfo valueType, @Nullable NameChecker nameChecker) throws ScriptParsingException {
		parser.beginCodeBlock();
		if (parser.input.peekAfterWhitespace() == ')') {
			parser.endCodeBlock();
			return new PrefixedNamedValues(null, NamedValue.EMPTY_ARRAY, false);
		}
		InsnTree prefix;
		CursorPos cursor = parser.input.getCursor();
		String firstName = parser.input.readIdentifierOrNullAfterWhitespace();
		List<NamedValue> namedValues;
		if (firstName != null && parser.input.hasOperatorAfterWhitespace(":")) {
			prefix = null;
			if (nameChecker != null) nameChecker.checkName(parser, firstName);
			InsnTree value = parser.nextScript();
			if (valueType != null) {
				value = value.cast(parser, valueType, CastMode.IMPLICIT_THROW);
			}
			if (parser.input.hasOperatorAfterWhitespace(",")) {
				namedValues = new ArrayList<>(8);
				namedValues.add(new NamedValue(firstName, value));
				//goto loop.
			}
			else if (parser.input.peekAfterWhitespace() == ')') {
				boolean hasNewVariables = parser.endCodeBlock();
				return new PrefixedNamedValues(null, new NamedValue[] { new NamedValue(firstName, value) }, hasNewVariables);
			}
			else {
				throw new ScriptParsingException("Expected ',' or ')'", parser.input);
			}
		}
		else {
			parser.input.setCursor(cursor);
			prefix = parser.nextScript();
			if (prefixType != null) {
				prefix = prefix.cast(parser, prefixType, CastMode.IMPLICIT_THROW);
			}
			if (parser.input.hasOperatorAfterWhitespace(",")) {
				namedValues = new ArrayList<>(8);
				//goto loop.
			}
			else if (parser.input.peekAfterWhitespace() == ')') {
				boolean hasNewVariables = parser.endCodeBlock();
				return new PrefixedNamedValues(prefix, NamedValue.EMPTY_ARRAY, hasNewVariables);
			}
			else {
				throw new ScriptParsingException("Expected ',' or ')'", parser.input);
			}
		}
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
		boolean hasNewVariables = parser.endCodeBlock();
		return new PrefixedNamedValues(prefix, namedValues.toArray(NamedValue.EMPTY_ARRAY), hasNewVariables);
	}

	@FunctionalInterface
	public static interface NameChecker {

		public abstract void checkName(ExpressionParser parser, String name) throws ScriptParsingException;
	}
}