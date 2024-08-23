package builderb0y.scripting.parsing.special;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.flow.compare.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.TypeMerger;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record CompareSyntax(InsnTree left, InsnTree right, TypeInfo inputType, InsnTree greaterThan, InsnTree lessThan, InsnTree equal, InsnTree incomparable, TypeInfo outputType) {

	public static CompareSyntax parse(ExpressionParser parser) throws ScriptParsingException {
		parser.beginCodeBlock();
		InsnTree left = parser.nextScript();
		InsnTree right;
		if (parser.input.hasOperatorAfterWhitespace(",")) {
			right = parser.nextScript();
			parser.input.expectOperatorAfterWhitespace(":");
		}
		else if (parser.input.hasOperatorAfterWhitespace(":")) {
			if (!left.getTypeInfo().isNumber()) {
				throw new ScriptParsingException("Implicit comparison to 0 requires value to be a number.", parser.input);
			}
			right = ldc(0, left.getTypeInfo());
		}
		else {
			throw new ScriptParsingException("Expected ',' or ':'", parser.input);
		}
		TypeInfo inputType = TypeMerger.computeMostSpecificType(left.getTypeInfo(), right.getTypeInfo());
		if (!inputType.isNumber() && !inputType.extendsOrImplements(TypeInfos.COMPARABLE)) {
			throw new ScriptParsingException("Can't compare " + left.getTypeInfo() + " and " + right.getTypeInfo(), parser.input);
		}
		left = left.cast(parser, inputType, CastMode.IMPLICIT_THROW);
		right = right.cast(parser, inputType, CastMode.IMPLICIT_THROW);
		boolean expectIncomparable = inputType.isFloat() || inputType.isObject();
		InsnTree greaterThan = null, lessThan = null, equalTo = null, incomparable = null;
		while (parser.input.hasIdentifierAfterWhitespace("case")) {
			parser.beginCodeBlock();
			switch (parser.input.readAfterWhitespace()) {
				case '>' -> {
					parser.input.expectOperatorAfterWhitespace(":");
					if (greaterThan != null) {
						throw new ScriptParsingException("Case '>' already specified", parser.input);
					}
					greaterThan = parser.nextScript();
				}
				case '<' -> {
					parser.input.expectOperatorAfterWhitespace(":");
					if (lessThan != null) {
						throw new ScriptParsingException("Case '<' already specified", parser.input);
					}
					lessThan = parser.nextScript();
				}
				case '=' -> {
					parser.input.expectOperatorAfterWhitespace(":");
					if (equalTo != null) {
						throw new ScriptParsingException("Case '=' already specified", parser.input);
					}
					equalTo = parser.nextScript();
				}
				case '!' -> {
					if (!expectIncomparable) {
						throw new ScriptParsingException("Case '!' is unreachable for " + left.getTypeInfo(), parser.input);
					}
					parser.input.expectOperatorAfterWhitespace(":");
					if (incomparable != null) {
						throw new ScriptParsingException("Case '!' already specified", parser.input);
					}
					incomparable = parser.nextScript();
				}
				default -> throw new ScriptParsingException(
					expectIncomparable
						? "Expected '>', '<', '=', or '!'"
						: "Expected '>', '<', or '='",
					parser.input
				);
			}
			parser.endCodeBlock();
		}
		parser.endCodeBlock();
		if (greaterThan == null) throw new ScriptParsingException("Missing case '>'", parser.input);
		if (lessThan == null) throw new ScriptParsingException("Missing case '<'", parser.input);
		if (equalTo == null) throw new ScriptParsingException("Missing case '='", parser.input);
		if (incomparable == null && expectIncomparable) throw new ScriptParsingException("Missing case '!'", parser.input);
		TypeInfo outputType = (
			expectIncomparable
				? TypeMerger.computeMostSpecificType(greaterThan.getTypeInfo(), lessThan.getTypeInfo(), equalTo.getTypeInfo(), incomparable.getTypeInfo())
				: TypeMerger.computeMostSpecificType(greaterThan.getTypeInfo(), lessThan.getTypeInfo(), equalTo.getTypeInfo())
		);
		greaterThan = greaterThan.cast(parser, outputType, CastMode.IMPLICIT_THROW);
		lessThan = lessThan.cast(parser, outputType, CastMode.IMPLICIT_THROW);
		equalTo = equalTo.cast(parser, outputType, CastMode.IMPLICIT_THROW);
		if (expectIncomparable) incomparable = incomparable.cast(parser, outputType, CastMode.IMPLICIT_THROW);
		return new CompareSyntax(left, right, inputType, greaterThan, lessThan, equalTo, incomparable, outputType);
	}

	public InsnTree buildInsnTree() {
		return switch (this.inputType.getSort()) {
			case BYTE, CHAR, SHORT, INT -> {
				if (this.right.getConstantValue().isConstant() && this.right.getConstantValue().asInt() == 0) {
					yield new IntCompareZeroInsnTree(this.left, this.lessThan, this.equal, this.greaterThan, this.outputType);
				}
				else {
					yield new IntCompareInsnTree(this.left, this.right, this.lessThan, this.equal, this.greaterThan, this.outputType);
				}
			}
			case LONG -> new LongCompareInsnTree(this.left, this.right, this.lessThan, this.equal, this.greaterThan, this.outputType);
			case FLOAT -> new FloatCompareInsnTree(this.left, this.right, this.lessThan, this.equal, this.greaterThan, this.incomparable, this.outputType);
			case DOUBLE -> new DoubleCompareInsnTree(this.left, this.right, this.lessThan, this.equal, this.greaterThan, this.incomparable, this.outputType);
			case OBJECT -> new ObjectCompareInsnTree(this.left, this.right, this.lessThan, this.equal, this.greaterThan, this.incomparable, this.outputType);
			case BOOLEAN, VOID, ARRAY -> throw new IllegalStateException(this.inputType.toString());
		};
	}
}