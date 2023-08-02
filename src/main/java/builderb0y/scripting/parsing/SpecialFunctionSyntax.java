package builderb0y.scripting.parsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.flow.AbstractForIteratorInsnTree;
import builderb0y.scripting.bytecode.tree.flow.ForIteratorInsnTree;
import builderb0y.scripting.bytecode.tree.flow.ForMapIteratorInsnTree;
import builderb0y.scripting.bytecode.tree.flow.ForRandomAccessListInsnTree;
import builderb0y.scripting.bytecode.tree.flow.compare.*;
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

		public static SwitchBody parse(ExpressionParser parser) throws ScriptParsingException {
			parser.input.expectAfterWhitespace('(');
			parser.environment.user().push();
			InsnTree value = parser.nextScript();
			parser.input.expectAfterWhitespace(':');
			Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
			IntArrayList builder = new IntArrayList(1);
			while (!parser.input.hasAfterWhitespace(')')) {
				if (parser.input.hasAfterWhitespace("case")) {
					parser.input.expectAfterWhitespace('(');
					parser.environment.user().push();
					do builder.add(nextConstantInt(parser));
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

		public static int nextConstantInt(ExpressionParser parser) throws ScriptParsingException {
			ConstantValue value = parser.nextSingleExpression().getConstantValue();
			if (value.isConstant() && value.getTypeInfo().isSingleWidthInt()) {
				return value.asInt();
			}
			else {
				throw new ScriptParsingException("Expected constant int", parser.input);
			}
		}
	}

	public static interface ForLoop {

		public abstract InsnTree buildLoop(ExpressionParser parser);

		public static ForLoop parse(ExpressionParser parser) throws ScriptParsingException {
			String loopName = parser.input.readIdentifierOrNullAfterWhitespace();
			parser.input.expectAfterWhitespace('(');
			parser.environment.user().push();
			CursorPos afterOpen = parser.input.getCursor();
			String firstTypeName = parser.input.readIdentifierOrNullAfterWhitespace();
			CursorPos firstTypeRevert = parser.input.getCursor();
			String firstVarName = parser.input.readIdentifierOrNullAfterWhitespace();
			CursorPos firstVarRevert = parser.input.getCursor();
			if (firstTypeName != null && firstVarName != null) {
				if (parser.input.hasIdentifierAfterWhitespace("in")) {
					TypeInfo firstType = parser.environment.getType(parser, firstTypeName);
					if (firstType == null) {
						parser.input.setCursor(firstTypeRevert);
						throw new ScriptParsingException("Unknown type: " + firstTypeName, parser.input);
					}
					CursorPos afterIn = parser.input.getCursor();
					parser.input.setCursor(firstVarRevert);
					parser.verifyName(firstTypeName, "variable");
					parser.input.setCursor(afterIn);
					VarInfo iterator = parser.environment.user().newAnonymousVariable(TypeInfos.ITERATOR);
					VarInfo userVar = parser.environment.user().newVariable(firstVarName, firstType);
					InsnTree rawIterable = parser.nextScript();
					InsnTree iterable;
					ForEachLoop.Mode mode;
					if (rawIterable.getTypeInfo().extendsOrImplements(type(List.class)) && rawIterable.getTypeInfo().extendsOrImplements(type(RandomAccess.class))) {
						iterable = rawIterable;
						mode = ForEachLoop.Mode.RANDOM_ACCESS_LIST;
					}
					else if (rawIterable.getTypeInfo().extendsOrImplements(type(Iterable.class))) {
						iterable = invokeInstance(rawIterable, AbstractForIteratorInsnTree.ITERATOR);
						mode = ForEachLoop.Mode.ITERABLE;
					}
					else if (rawIterable.getTypeInfo().extendsOrImplements(type(Iterator.class))) {
						iterable = rawIterable;
						mode = ForEachLoop.Mode.ITERATOR;
					}
					else {
						throw new ScriptParsingException("in clause must implement Iterable or Iterator", parser.input);
					}
					parser.input.expectOperatorAfterWhitespace(":");
					InsnTree body = parser.nextScript();
					parser.input.expectAfterWhitespace(')');
					parser.environment.user().pop();
					return new ForEachLoop(
						loopName,
						new VariableDeclareAssignInsnTree(iterator, iterable),
						mode,
						new VariableDeclarationInsnTree(userVar),
						body.asStatement()
					);
				}
				else if (parser.input.hasOperatorAfterWhitespace(",")) {
					String secondTypeName = parser.input.readIdentifierOrNullAfterWhitespace();
					CursorPos secondTypeRevert = parser.input.getCursor();
					String secondVarName = parser.input.readIdentifierOrNullAfterWhitespace();
					CursorPos secondVarRevert = parser.input.getCursor();
					if (secondTypeName != null && secondVarName != null) {
						if (parser.input.hasIdentifierAfterWhitespace("in")) {
							CursorPos afterIn = parser.input.getCursor();
							TypeInfo firstType = parser.environment.getType(parser, firstTypeName);
							if (firstType == null) {
								parser.input.setCursor(firstTypeRevert);
								throw new ScriptParsingException("Unknown type: " + firstTypeName, parser.input);
							}
							parser.input.setCursor(firstVarRevert);
							parser.verifyName(firstVarName, "variable");
							parser.input.setCursor(afterIn);

							TypeInfo secondType = parser.environment.getType(parser, secondTypeName);
							if (secondType == null) {
								parser.input.setCursor(secondTypeRevert);
								throw new ScriptParsingException("Unknown type: " + secondTypeName, parser.input);
							}
							parser.input.setCursor(secondVarRevert);
							parser.verifyName(secondVarName, "variable");
							parser.input.setCursor(afterIn);

							VarInfo iterator = parser.environment.user().newAnonymousVariable(TypeInfos.ITERATOR);
							VarInfo firstVar = parser.environment.user().newVariable(firstVarName, firstType);
							VarInfo secondVar = parser.environment.user().newVariable(secondVarName, secondType);
							InsnTree map = parser.nextScript().cast(parser, TypeInfos.MAP, CastMode.IMPLICIT_THROW);
							parser.input.expectOperatorAfterWhitespace(":");
							InsnTree body = parser.nextScript();
							parser.input.expectAfterWhitespace(')');
							parser.environment.user().pop();
							return new ForMapLoop(
								loopName,
								new VariableDeclareAssignInsnTree(
									iterator,
									invokeInstance(
										invokeInstance(map, ForMapIteratorInsnTree.ENTRY_SET),
										AbstractForIteratorInsnTree.ITERATOR
									)
								),
								new VariableDeclarationInsnTree(firstVar),
								new VariableDeclarationInsnTree(secondVar),
								body.asStatement()
							);
						}
					}
				}
			}
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
			return new ManualForLoop(loopName, initializer, condition, incrementer, body.asStatement());
		}
	}

	public static record ManualForLoop(
		String loopName,
		InsnTree initializer,
		ConditionTree condition,
		InsnTree step,
		InsnTree body
	)
	implements ForLoop {

		@Override
		public InsnTree buildLoop(ExpressionParser parser) {
			return for_(this.loopName, this.initializer, this.condition, this.step, this.body);
		}
	}

	public static record ForEachLoop(
		String loopName,
		VariableDeclareAssignInsnTree iteratorOrList,
		Mode mode,
		VariableDeclarationInsnTree userVar,
		InsnTree body
	)
	implements ForLoop {

		@Override
		public InsnTree buildLoop(ExpressionParser parser) {
			return (
				this.mode == Mode.RANDOM_ACCESS_LIST
				? new ForRandomAccessListInsnTree(this.loopName, this.userVar, this.iteratorOrList, this.body)
				: new ForIteratorInsnTree(this.loopName, this.userVar, this.iteratorOrList, this.body)
			);
		}

		public static enum Mode {
			RANDOM_ACCESS_LIST,
			ITERABLE,
			ITERATOR;
		}
	}

	public static record ForMapLoop(
		String loopName,
		VariableDeclareAssignInsnTree iterator,
		VariableDeclarationInsnTree keyVar,
		VariableDeclarationInsnTree valueVar,
		InsnTree body
	)
	implements ForLoop {

		@Override
		public InsnTree buildLoop(ExpressionParser parser) {
			return new ForMapIteratorInsnTree(this.loopName, this.keyVar, this.valueVar, this.iterator, this.body);
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

		public static NamedValues parse(ExpressionParser parser, @Nullable TypeInfo valueType) throws ScriptParsingException {
			parser.beginCodeBlock();
			List<NamedValue> namedValues = new ArrayList<>(8);
			if (parser.input.peekAfterWhitespace() != ')') {
				while (true) {
					String name = parser.input.expectIdentifierAfterWhitespace();
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
}