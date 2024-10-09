package builderb0y.scripting.parsing.special;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree.ElvisEmitters;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record SwitchBodySyntax(InsnTree value, Int2ObjectSortedMap<InsnTree> cases, boolean hasNewVariables) implements CodeBlock {

	public static final MethodInfo ENUM_ORDINAL = MethodInfo.getMethod(Enum.class, "ordinal");

	public static SwitchBodySyntax parse(ExpressionParser parser) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		parser.environment.user().push();
		InsnTree value = parser.nextScript();
		@SuppressWarnings("rawtypes")
		Class<? extends Enum> enumClass;
		if (value.getTypeInfo().extendsOrImplements(type(Enum.class))) {
			enumClass = value.getTypeInfo().toClass().asSubclass(Enum.class);
			value = new ElvisGetInsnTree(ElvisEmitters.forGetter(value, ENUM_ORDINAL, ldc(-1)));
		}
		else if (value.getTypeInfo().isSingleWidthInt()) {
			enumClass = null;
		}
		else {
			throw new ScriptParsingException("Switch value must be enum or single-width int, but it was " + value.getTypeInfo(), parser.input);
		}
		parser.input.expectAfterWhitespace(':');
		Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
		IntArrayList builder = new IntArrayList(1);
		while (!parser.input.hasAfterWhitespace(')')) {
			if (parser.input.hasIdentifierAfterWhitespace("case")) {
				parser.input.expectAfterWhitespace('(');
				parser.environment.user().push();
				do {
					if (parser.input.hasIdentifierAfterWhitespace("range")) {
						boolean lowerBoundInclusive;
						if (parser.input.hasAfterWhitespace("[")) lowerBoundInclusive = true;
						else if (parser.input.hasAfterWhitespace("(")) lowerBoundInclusive = false;
						else throw new ScriptParsingException("Expected '[' or '('", parser.input);
						long lowerBound = nextConstantInt(parser, enumClass);
						parser.input.expectOperatorAfterWhitespace(",");
						long upperBound = nextConstantInt(parser, enumClass);
						boolean upperBoundInclusive;
						if (parser.input.hasAfterWhitespace("]")) upperBoundInclusive = true;
						else if (parser.input.hasAfterWhitespace(")")) upperBoundInclusive = false;
						else throw new ScriptParsingException("Expected ']' or ')'", parser.input);
						if (!lowerBoundInclusive) lowerBound++;
						if (!upperBoundInclusive) upperBound--;
						if (upperBound < lowerBound) throw new ScriptParsingException("Empty range", parser.input);
						for (long case_ = lowerBound; case_ <= upperBound; case_++) {
							builder.add((int)(case_));
						}
					}
					else {
						builder.add(nextConstantInt(parser, enumClass));
					}
				}
				while (parser.input.hasOperatorAfterWhitespace(","));
				parser.input.expectOperatorAfterWhitespace(":");
				InsnTree body = parser.nextScript();
				parser.input.expectAfterWhitespace(')');
				if (parser.environment.user().hasNewVariables()) {
					body = scoped(body);
				}
				parser.environment.user().pop();

				int[] elements = builder.elements();
				int size = builder.size();
				for (int index = 0; index < size; index++) {
					if (cases.putIfAbsent(elements[index], body) != null) {
						throw new ScriptParsingException("Duplicate case: " + elements[index], parser.input);
					}
				}
				builder.clear();
			}
			else if (parser.input.hasIdentifierAfterWhitespace("default")) {
				if (cases.defaultReturnValue() != null) {
					throw new ScriptParsingException("Duplicate default", parser.input);
				}
				parser.input.expectAfterWhitespace('(');
				parser.environment.user().push();
				InsnTree body = parser.nextScript();
				parser.input.expectAfterWhitespace(')');
				if (parser.environment.user().hasNewVariables()) {
					body = scoped(body);
				}
				parser.environment.user().pop();
				cases.defaultReturnValue(body);
			}
			else {
				throw new ScriptParsingException("Expected 'case' or 'default'", parser.input);
			}
		}
		boolean newVariables = parser.environment.user().hasNewVariables();
		parser.environment.user().pop();
		return new SwitchBodySyntax(value, cases, newVariables);
	}

	@SuppressWarnings("unchecked")
	public static int nextConstantInt(ExpressionParser parser, @SuppressWarnings("rawtypes") Class<? extends Enum> enumClass) throws ScriptParsingException {
		if (enumClass != null) {
			String name = parser.input.readIdentifierOrNullAfterWhitespace();
			if (name == null) throw new ScriptParsingException("Expected enum constant name", parser.input);
			if (name.equals("null")) return -1;
			return Enum.valueOf(enumClass, name).ordinal();
		}
		else {
			ConstantValue value = parser.nextSingleExpression().getConstantValue();
			if (value.isConstant() && value.getTypeInfo().isSingleWidthInt()) {
				return value.asInt();
			}
			else {
				throw new ScriptParsingException("Expected constant int", parser.input);
			}
		}
	}
}