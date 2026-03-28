package builderb0y.scripting.parsing.special;

import java.util.ArrayList;
import java.util.List;

import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record MultiParameterSyntax(String[] names) {

	public static MultiParameterSyntax parse(ExpressionParser parser) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		if (parser.input.hasAfterWhitespace(')')) {
			return new MultiParameterSyntax(new String[0]);
		}
		List<String> parameters = new ArrayList<>();
		while (true) {
			String name = parser.verifyName(parser.input.expectIdentifierAfterWhitespace(), "parameter");
			parameters.add(name);
			if (parser.input.hasAfterWhitespace(')')) {
				return new MultiParameterSyntax(parameters.toArray(new String[parameters.size()]));
			}
			parser.input.hasOperatorAfterWhitespace(",");
		}
	}
}