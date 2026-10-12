package builderb0y.scripting.parsing.special;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record IsSyntax(TypeInfo type, @Nullable String name) {

	public static IsSyntax parse(ExpressionParser parser) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		String typeName = parser.input.expectIdentifierAfterWhitespace();
		TypeInfo typeInfo = parser.environment.getType(parser, typeName);
		if (typeInfo == null) throw new ScriptParsingException("Unknown type: " + typeName, parser.input);
		String name = parser.input.readIdentifierOrNullAfterWhitespace();
		if (name != null) {
			parser.verifyName(name, "variable");
		}
		parser.input.expectAfterWhitespace(')');
		return new IsSyntax(typeInfo, name);
	}
}