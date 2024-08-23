package builderb0y.scripting.parsing.special;

import java.util.*;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.loops.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionReader.CursorPos;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ForLoopSyntax {

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
			if (parser.input.hasOperatorAfterWhitespace("*")) {
				for (String varName : MultiParameterSyntax.parse(parser).names()) {
					parser.environment.user().reserveVariable(varName, type);
					variables.add(new LazyVarInfo(varName, type));
				}
			}
			else {
				String varName = parser.input.readIdentifierOrNullAfterWhitespace();
				if (varName == null) return null;
				parser.environment.user().reserveVariable(varName, type);
				variables.add(new LazyVarInfo(varName, type));
			}
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

	public static @Nullable RangeLoopFactory tryParseRange(ExpressionParser parser) throws ScriptParsingException {
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