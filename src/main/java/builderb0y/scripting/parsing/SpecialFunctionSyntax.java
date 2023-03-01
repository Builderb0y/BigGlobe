package builderb0y.scripting.parsing;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.ScopedInsnTree;
import builderb0y.scripting.parsing.ExpressionReader.CursorPos;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SpecialFunctionSyntax {

	public static interface CodeBlock {

		public abstract boolean hasNewVariables();

		public default InsnTree maybeWrap(InsnTree tree) {
			return this.hasNewVariables() ? new ScopedInsnTree(tree) : tree;
		}
	}

	public static record ParenthesizedScript(InsnTree contents, boolean hasNewVariables) implements CodeBlock {

		public static ParenthesizedScript parse(ExpressionParser parser) throws ScriptParsingException {
			parser.input.expectAfterWhitespace('(');
			CursorPos openParentheses = parser.input.getCursor();
			if (!parser.input.canReadAfterWhitespace()) {
				parser.input.setCursor(openParentheses);
				throw new ScriptParsingException("Unmatched parentheses", parser.input);
			}
			parser.environment.user().push();
			InsnTree result = parser.nextScript();
			if (!parser.input.canReadAfterWhitespace()) {
				parser.input.setCursor(openParentheses);
				throw new ScriptParsingException("Unmatched parentheses", parser.input);
			}
			parser.input.expect(')');
			boolean newVariables = parser.environment.user().hasNewVariables();
			parser.environment.user().pop();
			return new ParenthesizedScript(result, newVariables);
		}

		public InsnTree maybeWrapContents() {
			return this.maybeWrap(this.contents);
		}
	}

	public static record CommaSeparatedExpressions(InsnTree[] arguments, boolean hasNewVariables) implements CodeBlock {

		public static CommaSeparatedExpressions parse(ExpressionParser parser) throws ScriptParsingException {
			parser.input.expectAfterWhitespace('(');
			CursorPos openParentheses = parser.input.getCursor();
			if (!parser.input.canReadAfterWhitespace()) {
				parser.input.setCursor(openParentheses);
				throw new ScriptParsingException("Unmatched parentheses", parser.input);
			}
			if (parser.input.hasAfterWhitespace(')')) {
				return new CommaSeparatedExpressions(InsnTree.ARRAY_FACTORY.empty(), false);
			}
			parser.environment.user().push();
			List<InsnTree> args = new ArrayList<>(4);
			while (true) {
				args.add(parser.nextScript());
				if (!parser.input.canReadAfterWhitespace()) {
					parser.input.setCursor(openParentheses);
					throw new ScriptParsingException("Unmatched parentheses", parser.input);
				}
				if (parser.input.hasAfterWhitespace(',')) {
					continue;
				}
				else if (parser.input.hasAfterWhitespace(')')) {
					break;
				}
			}
			boolean newVariables = parser.environment.user().hasNewVariables();
			parser.environment.user().pop();
			return new CommaSeparatedExpressions(InsnTree.ARRAY_FACTORY.collectionToArray(args), newVariables);
		}
	}

	public static record ConditionBody(ConditionTree condition, InsnTree body, boolean hasNewVariables) implements CodeBlock {

		public InsnTree maybeWrapBody() {
			return this.maybeWrap(this.body);
		}

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
						body = new ScopedInsnTree(body);
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
						body = new ScopedInsnTree(body);
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
			ConstantValue value = parser.nextExpression().getConstantValue();
			if (value.isConstant() && value.getTypeInfo().isSingleWidthInt()) {
				return value.asInt();
			}
			else {
				throw new ScriptParsingException("Expected constant int", parser.input);
			}
		}
	}

	public static record ForLoop(InsnTree initializer, ConditionTree condition, InsnTree incrementer, InsnTree body, boolean hasNewVariables) implements CodeBlock {

		public static ForLoop parse(ExpressionParser parser) throws ScriptParsingException {
			parser.input.expectAfterWhitespace('(');
			parser.environment.user().push();
			InsnTree initializer = parser.nextScript();
			parser.input.expectOperatorAfterWhitespace(",");
			ConditionTree condition = InsnTrees.condition(parser, parser.nextScript());
			parser.input.expectOperatorAfterWhitespace(",");
			InsnTree incrementer = parser.nextScript();
			parser.input.expectOperatorAfterWhitespace(":");
			InsnTree body = parser.nextScript();
			parser.input.expectAfterWhitespace(')');
			boolean newVariables = parser.environment.user().hasNewVariables();
			parser.environment.user().pop();
			return new ForLoop(initializer, condition, incrementer, body, newVariables);
		}
	}

	public static record ForEachLoop(VariableDeclarationInsnTree iterator, VariableDeclarationInsnTree userVar, InsnTree iterable, InsnTree body) {

		public static @Nullable ForEachLoop tryParse(ExpressionParser parser) throws ScriptParsingException {
			CursorPos revert = parser.input.getCursor();
			parser.input.expectAfterWhitespace('(');
			String typeName = parser.input.readIdentifierAfterWhitespace();
			if (typeName.isEmpty()) {
				parser.input.setCursor(revert);
				return null;
			}
			CursorPos typeRevert = parser.input.getCursor();
			String name = parser.input.readIdentifierAfterWhitespace();
			if (name.isEmpty()) {
				parser.input.setCursor(revert);
				return null;
			}
			String in = parser.input.readIdentifierAfterWhitespace();
			if (!in.equals("in")) {
				parser.input.setCursor(revert);
				return null;
			}
			TypeInfo type = parser.environment.getType(parser, typeName);
			if (type == null) {
				parser.input.setCursor(typeRevert);
				throw new ScriptParsingException("Unknown type: " + typeName, parser.input);
			}
			parser.environment.user().push();
			VariableDeclarationInsnTree iterator = parser.environment.user().newVariable("iterator", TypeInfos.ITERATOR);
			VariableDeclarationInsnTree userVar = parser.environment.user().newVariable(name, type);
			InsnTree iterable = parser.nextScript().cast(parser, TypeInfos.ITERABLE, CastMode.IMPLICIT_THROW);
			parser.input.expectOperatorAfterWhitespace(":");
			InsnTree body = parser.nextScript();
			parser.input.expectAfterWhitespace(')');
			parser.environment.user().pop();
			return new ForEachLoop(iterator, userVar, iterable, body);
		}

		public InsnTree toLoop(ExpressionParser parser) {
			//(
			//	Iterator iterator
			//	Type userVar
			//	iterator = iterable.iterator()
			//	while (iterator.hasNext():
			//		userVar = iterator.next()
			//		body
			//	)
			//)
			InsnTree storeIterator = store(
				this.iterator.loader.variable,
				invokeInterface(
					this.iterable,
					method(ACC_PUBLIC | ACC_INTERFACE, TypeInfos.ITERABLE, "iterator", TypeInfos.ITERATOR)
				)
			);
			InsnTree hasNext = invokeInterface(
				load(this.iterator.loader.variable),
				method(ACC_PUBLIC | ACC_INTERFACE, TypeInfos.ITERATOR, "hasNext", TypeInfos.BOOLEAN)
			);
			InsnTree storeUserVar = store(
				this.userVar.loader.variable,
				invokeInterface(
					load(this.iterator.loader.variable),
					method(ACC_PUBLIC | ACC_INTERFACE, TypeInfos.ITERATOR, "next", TypeInfos.OBJECT)
				)
				.cast(parser, this.userVar.loader.variable.type, CastMode.EXPLICIT_THROW)
			);
			return scoped(
				seq(
					parser,
					this.iterator,
					this.userVar,
					storeIterator,
					while_(
						parser,
						condition(parser, hasNext),
						seq(
							parser,
							storeUserVar,
							this.body
						)
					)
				)
			);
			//for (Iterator iterator,, Type userVar,, iterator = iterable.iterator(), iterator.hasNext(), noop:
			//	userVar = iterator.next().as(Type)
			//	body
			//)
			/*
			return scoped(
				for_(
					parser,
					seq(parser, this.iterator, this.userVar, storeIterator),
					condition(parser, hasNext),
					noop,
					seq(parser, storeUserVar, this.body)
				)
			)
			*/
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
				String name = parser.input.expectIdentifierAfterWhitespace();
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
			parser.input.expectAfterWhitespace('(');
			parser.environment.user().push();
			List<NamedValue> namedValues = new ArrayList<>(8);
			if (!parser.input.hasAfterWhitespace(')')) {
				while (true) {
					String name = parser.input.expectIdentifierAfterWhitespace();
					parser.input.expectOperatorAfterWhitespace(":");
					InsnTree value = parser.nextScript();
					if (valueType != null) {
						value = value.cast(parser, valueType, CastMode.IMPLICIT_THROW);
					}
					namedValues.add(new NamedValue(name, value));
					if (parser.input.hasOperatorAfterWhitespace(",")) continue;
					else if (parser.input.hasAfterWhitespace(')')) break;
					else throw new ScriptParsingException("Expected ',' or ')'", parser.input);
				}
			}
			boolean hasNewVariables = parser.environment.user().hasNewVariables();
			parser.environment.user().pop();
			return new NamedValues(namedValues.toArray(new NamedValue[namedValues.size()]), hasNewVariables);
		}
	}
}