package builderb0y.scripting.parsing.special;

import java.util.ArrayList;
import java.util.List;

import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public record ConstantMapSyntax(ConstantValue... keysAndValues) {

	public static ConstantMapSyntax parse(ExpressionParser parser) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		if (parser.input.hasAfterWhitespace(')')) {
			return new ConstantMapSyntax();
		}
		List<ConstantValue> keysAndValues = new ArrayList<>();
		while (true) {
			InsnTree key = parser.nextScript();
			ConstantValue constantKey = key.getConstantValue();
			if (constantKey == null) throw new ScriptParsingException("Not a constant key: " + key.describe(), parser.input);
			parser.input.expectOperatorAfterWhitespace(":");
			InsnTree value = parser.nextScript();
			ConstantValue constantValue = value.getConstantValue();
			if (!constantValue.isConstantOrDynamic()) throw new ScriptParsingException("Not a constant value: " + value.describe(), parser.input);
			keysAndValues.add(constantKey);
			keysAndValues.add(constantValue);
			if (parser.input.hasOperatorAfterWhitespace(",")) {
				continue;
			}
			else if (parser.input.hasAfterWhitespace(')')) {
				break;
			}
			else {
				throw new ScriptParsingException("Expected ',' or ')'", parser.input);
			}
		}
		return new ConstantMapSyntax(keysAndValues.toArray(new ConstantValue[keysAndValues.size()]));
	}
}