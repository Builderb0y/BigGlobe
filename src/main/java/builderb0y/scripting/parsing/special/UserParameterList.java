package builderb0y.scripting.parsing.special;

import java.util.ArrayList;
import java.util.List;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record UserParameterList(UserParameter... parameters) {

	public static record UserParameter(TypeInfo type, String name) {

		@Override
		public String toString() {
			return this.type.getClassName() + ' ' + this.name;
		}
	}

	public static UserParameterList parse(ExpressionParser parser) throws ScriptParsingException {
		if (parser.input.hasOperatorAfterWhitespace(":")) {
			return new UserParameterList();
		}
		List<UserParameter> parameters = new ArrayList<>(4);
		while (true) {
			String typeName = parser.input.expectIdentifierAfterWhitespace();
			TypeInfo type = parser.environment.getType(parser, typeName);
			if (type == null) throw new ScriptParsingException("Unknown type: " + typeName, parser.input);
			if (type.getSort() == Sort.VOID) {
				throw new ScriptParsingException("void-typed parameters are not allowed.", parser.input);
			}
			if (parser.input.hasOperatorAfterWhitespace("*")) {
				for (String name : MultiParameterSyntax.parse(parser).names()) {
					parameters.add(new UserParameter(type, name));
				}
			}
			else {
				String name = parser.verifyName(parser.input.expectIdentifierAfterWhitespace(), "parameter");
				parameters.add(new UserParameter(type, name));
			}
			if (parser.input.hasOperatorAfterWhitespace(",")) continue;
			else if (parser.input.hasOperatorAfterWhitespace(":")) break;
			else throw new ScriptParsingException("Expected ',' or ':'", parser.input);
		}
		return new UserParameterList(parameters.toArray(new UserParameter[parameters.size()]));
	}
}