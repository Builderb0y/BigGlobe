package builderb0y.scripting.parsing;

import java.util.*;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.loops.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.flow.compare.*;
import builderb0y.scripting.bytecode.tree.instructions.between.BetweenInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree.ElvisEmitters;
import builderb0y.scripting.parsing.ExpressionReader.CursorPos;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.TypeMerger;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SpecialFunctionSyntax {

	public static interface CodeBlock {

		public abstract boolean hasNewVariables();

		public default InsnTree maybeWrap(InsnTree tree) {
			return this.hasNewVariables() ? scoped(tree) : tree;
		}
	}

	public static record ParenthesizedScript(InsnTree contents, boolean hasNewVariables) implements CodeBlock {

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

	public static record CommaSeparatedExpressions(InsnTree[] arguments, boolean hasNewVariables) implements CodeBlock {

		public static CommaSeparatedExpressions parse(ExpressionParser parser) throws ScriptParsingException {
			parser.beginCodeBlock();
			if (parser.input.peekAfterWhitespace() == ')') {
				parser.endCodeBlock();
				return new CommaSeparatedExpressions(InsnTree.ARRAY_FACTORY.empty(), false);
			}
			List<InsnTree> args = new ArrayList<>(4);
			boolean newVariables;
			while (true) {
				args.add(parser.nextScript());
				if (parser.input.hasAfterWhitespace(',')) {
					continue;
				}
				else {
					newVariables = parser.endCodeBlock();
					break;
				}
			}
			return new CommaSeparatedExpressions(InsnTree.ARRAY_FACTORY.collectionToArray(args), newVariables);
		}
	}

	public static record ConditionBody(ConditionTree condition, InsnTree body, boolean hasNewVariables) implements CodeBlock {

		public static ConditionBody parse(ExpressionParser parser) throws ScriptParsingException {
			parser.input.expectAfterWhitespace('(');
			parser.environment.user().push();
			ConditionTree condition = InsnTrees.condition(parser, parser.nextScript());
			parser.input.expectOperatorAfterWhitespace(":");
			InsnTree body = parser.nextScript();
			boolean hasNewVariables = parser.environment.user().hasNewVariables();
			parser.environment.user().pop();
			parser.input.expectAfterWhitespace(')');
			return new ConditionBody(condition, body, hasNewVariables);
		}
	}

	public static record ScriptBody(InsnTree expression, InsnTree body, boolean hasNewVariables) implements CodeBlock {

		public static ScriptBody parse(ExpressionParser parser, HeaderProcessor headerProcessor) throws ScriptParsingException {
			parser.input.expectAfterWhitespace('(');
			parser.environment.user().push();
			InsnTree expression = headerProcessor.apply(parser.nextScript(), parser);
			parser.input.expectOperatorAfterWhitespace(":");
			InsnTree body = parser.nextScript();
			boolean hasNewVariables = parser.environment.user().hasNewVariables();
			parser.environment.user().pop();
			parser.input.expectAfterWhitespace(')');
			return new ScriptBody(expression, body, hasNewVariables);
		}

		@FunctionalInterface
		public static interface HeaderProcessor {

			public abstract InsnTree apply(InsnTree header, ExpressionParser parser) throws ScriptParsingException;
		}
	}

	public static record SwitchBody(InsnTree value, Int2ObjectSortedMap<InsnTree> cases, boolean hasNewVariables) implements CodeBlock {

		public static final MethodInfo ENUM_ORDINAL = MethodInfo.getMethod(Enum.class, "ordinal");

		public static SwitchBody parse(ExpressionParser parser) throws ScriptParsingException {
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
				throw new ScriptParsingException("Switch value must be enum or single-width int", parser.input);
			}
			parser.input.expectAfterWhitespace(':');
			Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
			IntArrayList builder = new IntArrayList(1);
			while (!parser.input.hasAfterWhitespace(')')) {
				if (parser.input.hasAfterWhitespace("case")) {
					parser.input.expectAfterWhitespace('(');
					parser.environment.user().push();
					do builder.add(nextConstantInt(parser, enumClass));
					while (parser.input.hasOperatorAfterWhitespace(","));
					parser.input.expectOperatorAfterWhitespace(":");
					InsnTree body = parser.nextScript();
					parser.input.expectAfterWhitespace(')');
					if (parser.environment.user().hasNewVariables()) {
						body = scoped(body);
					}
					parser.environment.user().pop();;

					int[] elements = builder.elements();
					int size = builder.size();
					for (int index = 0; index < size; index++) {
						if (cases.putIfAbsent(elements[index], body) != null) {
							throw new ScriptParsingException("Duplicate case: " + elements[index], parser.input);
						}
					}
					builder.clear();
				}
				else if (parser.input.hasAfterWhitespace("default")) {
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
					parser.environment.user().pop();;
					cases.defaultReturnValue(body);
				}
				else {
					throw new ScriptParsingException("Expected 'case' or 'default'", parser.input);
				}
			}
			boolean newVariables = parser.environment.user().hasNewVariables();
			parser.environment.user().pop();
			return new SwitchBody(value, cases, newVariables);
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

	public static class ForLoop {

		public static InsnTree parse(ExpressionParser parser) throws ScriptParsingException {
			LoopName loopName = LoopName.of(parser.input.readIdentifierOrNullAfterWhitespace());
			parser.input.expectAfterWhitespace('(');
			parser.environment.user().push();
			CursorPos afterOpen = parser.input.getCursor();
			InsnTree result = tryParseEnhanced(parser, loopName);
			if (result != null) {
				parser.input.expectAfterWhitespace(')');
				parser.environment.user().pop();
				return result;
			}
			else {
				//clear any local variables tryParseEnhanced() may have created.
				parser.environment.user().pop();
				parser.environment.user().push();

				parser.input.setCursor(afterOpen);
				InsnTree initializer = parser.nextScript();
				parser.input.expectOperatorAfterWhitespace(",");
				ConditionTree condition = condition(parser, parser.nextScript());
				parser.input.expectOperatorAfterWhitespace(",");
				InsnTree incrementer = parser.nextScript();
				parser.input.expectOperatorAfterWhitespace(":");
				InsnTree body = parser.nextScript();
				parser.input.expectAfterWhitespace(')');
				parser.environment.user().pop();
				return for_(loopName, initializer, condition, incrementer, body.asStatement());
			}
		}

		public static @Nullable InsnTree tryParseEnhanced(ExpressionParser parser, LoopName loopName) throws ScriptParsingException {
			List<LazyVarInfo> variables = new ArrayList<>(4);
			variables:
			while (true) {
				String typeName = parser.input.readIdentifierOrNullAfterWhitespace();
				if (typeName == null) return null;
				TypeInfo type = parser.environment.getType(parser, typeName);
				if (type == null) return null;
				String varName = parser.input.readIdentifierOrNullAfterWhitespace();
				if (varName == null) return null;
				parser.environment.user().reserveVariable(varName, type);
				variables.add(new LazyVarInfo(varName, type));
				if (parser.input.hasIdentifierAfterWhitespace("in")) {
					LoopFactory loopFactory = tryParseRange(parser);
					if (loopFactory == null) {
						InsnTree iterable = parser.nextScript();
						if (iterable.getTypeInfo().extendsOrImplements(type(Iterable.class))) {
							if (iterable.getTypeInfo().extendsOrImplements(type(List.class))) {
								if (iterable.getTypeInfo().extendsOrImplements(type(RandomAccess.class))) {
									loopFactory = new RandomAccessListLoopFactory(iterable);
								}
								else {
									loopFactory = new SequentialListLoopFactory(iterable);
								}
							}
							else {
								loopFactory = new IterableLoopFactory(iterable);
							}
						}
						else if (iterable.getTypeInfo().extendsOrImplements(type(Map.class))) {
							loopFactory = new MapLoopFactory(iterable);
						}
						else if (iterable.getTypeInfo().extendsOrImplements(type(Iterator.class))) {
							loopFactory = new IteratorLoopFactory(iterable);
						}
						else {
							throw new ScriptParsingException("in clause must be of type Iterable, Iterator, Map, or range", parser.input);
						}
					}
					List<VariableDeclarationInsnTree> declarations = variables.stream().peek((LazyVarInfo variable) -> parser.environment.user().assignVariable(variable.name)).map(VariableDeclarationInsnTree::new).toList();
					return switch (parser.input.readOperatorAfterWhitespace()) {
						case ":" -> loopFactory.createLoop(parser, loopName, declarations, parser.nextScript().asStatement());
						case "," -> {
							InsnTree body = tryParseEnhanced(parser, loopName);
							if (body == null) throw new ScriptParsingException("Expected next variable declaration", parser.input);
							yield loopFactory.createLoop(parser, loopName, declarations, body);
						}
						default -> throw new ScriptParsingException("Expected ':' or ','", parser.input);
					};
				}
				else if (parser.input.hasOperatorAfterWhitespace(",")) {
					continue variables;
				}
				else {
					return null;
				}
			}
		}

		public static @Nullable RangeLoopFactory tryParseRange(ExpressionParser parser) throws ScriptParsingException{
			CursorPos afterIn = parser.input.getCursor();
			boolean hasMinus = parser.input.hasOperatorAfterWhitespace("-");
			if (parser.input.hasIdentifierAfterWhitespace("range")) {
				boolean lowerBoundInclusive = switch (parser.input.readAfterWhitespace()) {
					case '[' -> true;
					case '(' -> false;
					default -> throw new ScriptParsingException("Expected '[' or '('", parser.input);
				};
				parser.environment.user().push();
				InsnTree lowerBound = parser.nextScript();
				lowerBound = lowerBound.cast(parser, TypeInfos.widenToInt(lowerBound.getTypeInfo()), CastMode.IMPLICIT_THROW);
				parser.input.expectOperatorAfterWhitespace(",");
				InsnTree upperBound = parser.nextScript();
				upperBound = upperBound.cast(parser, TypeInfos.widenToInt(upperBound.getTypeInfo()), CastMode.IMPLICIT_THROW);
				if (upperBound.getTypeInfo().getSort() != lowerBound.getTypeInfo().getSort()) {
					throw new ScriptParsingException("Range bounds must have the same type", parser.input);
				}
				boolean upperBoundInclusive = switch (parser.input.readAfterWhitespace()) {
					case ']' -> true;
					case ')' -> false;
					default -> throw new ScriptParsingException("Expected ']' or ')'", parser.input);
				};
				parser.environment.user().pop();
				InsnTree step;
				if (parser.input.hasOperatorAfterWhitespace("%")) {
					step = parser.nextExponent();
					step = step.cast(parser, TypeInfos.widenToInt(step.getTypeInfo()), CastMode.IMPLICIT_THROW);
					if (step.getTypeInfo().getSort() != lowerBound.getTypeInfo().getSort()) {
						throw new ScriptParsingException("Step type must match bound types", parser.input);
					}
				}
				else {
					step = ldc(1, lowerBound.getTypeInfo());
				}
				return new RangeLoopFactory(
					!hasMinus,
					lowerBound,
					lowerBoundInclusive,
					upperBound,
					upperBoundInclusive,
					step
				);
			}
			else {
				parser.input.setCursor(afterIn);
				return null;
			}
		}
	}

	public static record UserParameterList(UserParameter... parameters) {

		public static record UserParameter(TypeInfo type, String name) {}

		public static UserParameterList parse(ExpressionParser parser) throws ScriptParsingException {
			if (parser.input.hasOperatorAfterWhitespace(":")) {
				return new UserParameterList();
			}
			List<UserParameter> parameters = new ArrayList<>(4);
			while (true) {
				String typeName = parser.input.expectIdentifierAfterWhitespace();
				TypeInfo type = parser.environment.getType(parser, typeName);
				if (type == null) throw new ScriptParsingException("Unknown type: " + typeName, parser.input);
				String name = parser.verifyName(parser.input.expectIdentifierAfterWhitespace(), "parameter");
				parameters.add(new UserParameter(type, name));
				if (parser.input.hasOperatorAfterWhitespace(",")) continue;
				else if (parser.input.hasOperatorAfterWhitespace(":")) break;
				else throw new ScriptParsingException("Expected ',' or ':'", parser.input);
			}
			return new UserParameterList(parameters.toArray(new UserParameter[parameters.size()]));
		}
	}

	public static record NamedValues(NamedValue[] values, boolean hasNewVariables) implements CodeBlock {

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

	public static record Compare(InsnTree left, InsnTree right, TypeInfo inputType, InsnTree greaterThan, InsnTree lessThan, InsnTree equal, InsnTree incomparable, TypeInfo outputType) {

		public static Compare parse(ExpressionParser parser) throws ScriptParsingException {
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
			return new Compare(left, right, inputType, greaterThan, lessThan, equalTo, incomparable, outputType);
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

	public static record IsBetween(InsnTree value, InsnTree min, boolean minInclusive, InsnTree max, boolean maxInclusive) {

		public static IsBetween parse(ExpressionParser parser, InsnTree receiver) throws ScriptParsingException {
			TypeInfo expectedType = TypeInfos.widenToInt(receiver.getTypeInfo());
			if (!expectedType.isNumber()) {
				throw new ScriptParsingException("Value must be numeric", parser.input);
			}
			boolean minInclusive = switch (parser.input.readAfterWhitespace()) {
				case '[' -> true;
				case '(' -> false;
				default -> throw new ScriptParsingException("Expected '[' or '('", parser.input);
			};
			parser.environment.user().push();
			InsnTree min = parser.nextScript().cast(parser, expectedType, CastMode.IMPLICIT_THROW);
			parser.input.expectOperatorAfterWhitespace(",");
			InsnTree max = parser.nextScript().cast(parser, expectedType, CastMode.IMPLICIT_THROW);
			boolean maxInclusive = switch (parser.input.readAfterWhitespace()) {
				case ']' -> true;
				case ')' -> false;
				default -> throw new ScriptParsingException("Expected ']' or ')'", parser.input);
			};
			parser.environment.user().pop();
			return new IsBetween(receiver, min, minInclusive, max, maxInclusive);
		}

		public InsnTree toTree(ExpressionParser parser) {
			return BetweenInsnTree.create(parser, this.value, this.min, this.minInclusive, this.max, this.maxInclusive);
		}
	}
}