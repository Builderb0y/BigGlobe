package builderb0y.scripting.environments;

import java.io.PrintStream;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatFactory;
import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.flow.WhileInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.BlockInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.BreakInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ContinueInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.NoopInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.*;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BuiltinScriptEnvironment implements ScriptEnvironment {

	public static final MethodInfo
		STRING_CONCAT_FACTORY = method(
			ACC_PUBLIC | ACC_STATIC,
			StringConcatFactory.class,
			"makeConcat",
			CallSite.class,
			MethodHandles.Lookup.class,
			String.class,
			MethodType.class
		),
		PRINTLN = method(
			ACC_PUBLIC,
			PrintStream.class,
			"println",
			void.class,
			String.class
		);
	public static final FieldInfo
		SYSTEM_OUT = field(
			ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
			System.class,
			"out",
			PrintStream.class
		);
	public static final BuiltinScriptEnvironment
		INSTANCE = new BuiltinScriptEnvironment();

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		return switch (name) {
			case "yes", "true" -> ldc(true);
			case "no", "false" -> ldc(false);
			case "noop"        -> NoopInsnTree.INSTANCE;
			case "null"        -> ldc(null, TypeInfos.OBJECT);
			default            -> null;
		};
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		return switch (name) {
			case "return" -> parser.createReturn(
				switch (arguments.length) {
					case 0 -> noop;
					case 1 -> arguments[0];
					default -> throw new ScriptParsingException("Returning multiple values is not supported", parser.input);
				}
			);
			case "throw" -> throw_(ScriptEnvironment.castArgument(parser, name, TypeInfos.THROWABLE, CastMode.IMPLICIT_THROW, arguments));
			case "print" -> {
				InsnTree loadOut = getStatic(SYSTEM_OUT);
				InsnTree concat = invokeDynamic(
					STRING_CONCAT_FACTORY,
					method(
						ACC_PUBLIC | ACC_STATIC,
						TypeInfos.OBJECT, //ignored
						"concat",
						TypeInfos.STRING,
						Arrays
						.stream(arguments)
						.map(InsnTree::getTypeInfo)
						.toArray(TypeInfo.ARRAY_FACTORY)
					),
					ConstantValue.ARRAY_FACTORY.empty(),
					arguments
				);
				yield invokeVirtual(loadOut, PRINTLN, concat);
			}
			default -> null;
		};
	}

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return switch (name) {
			case "boolean"    -> TypeInfos.BOOLEAN;
			case "byte"       -> TypeInfos.   BYTE;
			case "short"      -> TypeInfos.  SHORT;
			case "int"        -> TypeInfos.    INT;
			case "long"       -> TypeInfos.   LONG;
			case "float"      -> TypeInfos.  FLOAT;
			case "double"     -> TypeInfos. DOUBLE;
			case "char"       -> TypeInfos.   CHAR;
			case "void"       -> TypeInfos.   VOID;

			case "Boolean"    -> TypeInfos.BOOLEAN_WRAPPER;
			case "Byte"       -> TypeInfos.   BYTE_WRAPPER;
			case "Short"      -> TypeInfos.  SHORT_WRAPPER;
			case "Integer"    -> TypeInfos.    INT_WRAPPER;
			case "Long"       -> TypeInfos.   LONG_WRAPPER;
			case "Float"      -> TypeInfos.  FLOAT_WRAPPER;
			case "Double"     -> TypeInfos. DOUBLE_WRAPPER;
			case "Character"  -> TypeInfos.   CHAR_WRAPPER;
			case "Void"       -> TypeInfos.   VOID_WRAPPER;
			case "Number"     -> TypeInfos.NUMBER;

			case "Object"     -> TypeInfos.OBJECT;
			case "Comparable" -> TypeInfos.COMPARABLE;
			case "String"     -> TypeInfos.STRING;
			case "Throwable"  -> TypeInfos.THROWABLE;
			case "Class"      -> TypeInfos.CLASS; //todo: casting from String to Class. make sure this uses ScriptEnvironment.getType(), NOT Class.forName()!

			default           -> null;
		};
	}

	@Override
	public @Nullable InsnTree parseKeyword(ExpressionParser parser, String name) throws ScriptParsingException {
		return switch (name) {
			case "if" -> {
				ConditionBody ifStatement = ConditionBody.parse(parser);
				InsnTree elseStatement = this.nextElse(parser);
				yield (
					elseStatement != null
					? ifElse(
						parser,
						ifStatement.condition(),
						ifStatement.body(),
						elseStatement
					)
					: ifThen(
						parser,
						ifStatement.condition(),
						ifStatement.body()
					)
				);
			}
			case "unless" -> {
				ConditionBody ifStatement = ConditionBody.parse(parser);
				InsnTree elseStatement = this.nextElse(parser);
				yield (
					elseStatement != null
					? ifElse(
						parser,
						not(ifStatement.condition()),
						ifStatement.body(),
						elseStatement
					)
					: ifThen(
						parser,
						not(ifStatement.condition()),
						ifStatement.body()
					)
				);
			}
			case "while" -> {
				ConditionBody whileStatement = ConditionBody.parse(parser);
				yield while_(parser, whileStatement.condition(), whileStatement.body());
			}
			case "until" -> {
				ConditionBody whileStatement = ConditionBody.parse(parser);
				yield while_(parser, not(whileStatement.condition()), whileStatement.body());
			}
			case "do" -> switch (parser.input.readIdentifierAfterWhitespace()) {
				case "while" -> {
					ConditionBody whileStatement = ConditionBody.parse(parser);
					yield doWhile(parser, whileStatement.condition(), whileStatement.body());
				}
				case "until" -> {
					ConditionBody whileStatement = ConditionBody.parse(parser);
					yield doWhile(parser, not(whileStatement.condition()), whileStatement.body());
				}
				default -> throw new ScriptParsingException("Expected 'while' or 'until' after 'do'", parser.input);
			};
			case "repeat" -> {
				ScriptBody repeatStatement = ScriptBody.parse(parser, (count, parser1) -> count.cast(parser1, TypeInfos.INT, CastMode.IMPLICIT_THROW));
				yield WhileInsnTree.createRepeat(parser, repeatStatement.expression(), repeatStatement.body());
			}
			case "for" -> {
				ForEachLoop enhancedLoop = ForEachLoop.tryParse(parser);
				if (enhancedLoop != null) {
					yield enhancedLoop.toLoop(parser);
				}
				else {
					ForLoop loop = ForLoop.parse(parser);
					yield for_(parser, loop.initializer(), loop.condition(), loop.incrementer(), loop.body());
				}
			}
			case "switch" -> {
				SwitchBody switchBody = SwitchBody.parse(parser);
				yield switchBody.maybeWrap(switch_(parser, switchBody.value(), switchBody.cases()));
			}
			case "block" -> {
				ParenthesizedScript script = ParenthesizedScript.parse(parser);
				yield new BlockInsnTree(script.contents());
			}
			case "break" -> {
				parser.input.expectAfterWhitespace('(');
				parser.input.expectAfterWhitespace(')');
				yield BreakInsnTree.INSTANCE;
			}
			case "continue" -> {
				parser.input.expectAfterWhitespace('(');
				parser.input.expectAfterWhitespace(')');
				yield ContinueInsnTree.INSTANCE;
			}
			default -> null;
		};
	}

	@Override
	public @Nullable InsnTree parseMemberKeyword(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		return switch (name) {
			case "is" -> {
				if (!receiver.getTypeInfo().isObject()) {
					throw new ScriptParsingException("Can't check primitive.is()", parser.input);
				}
				TypeInfo type = this.nextParenthesizedType(parser);
				if (!type.isObject()) {
					throw new ScriptParsingException("Can't check object.is(primitive)", parser.input);
				}
				yield instanceOf(receiver, type);
			}
			case "isnt" -> {
				if (!receiver.getTypeInfo().isObject()) {
					throw new ScriptParsingException("Can't check primitive.isnt()", parser.input);
				}
				TypeInfo type = this.nextParenthesizedType(parser);
				if (!type.isObject()) {
					throw new ScriptParsingException("Can't check object.isnt(primitive)", parser.input);
				}
				yield not(parser, instanceOf(receiver, type));
			}
			case "as" -> {
				yield receiver.cast(parser, this.nextParenthesizedType(parser), CastMode.EXPLICIT_THROW);
			}
			default -> null;
		};
	}

	public TypeInfo nextParenthesizedType(ExpressionParser parser) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		String typeName = parser.input.expectIdentifierAfterWhitespace();
		TypeInfo type = parser.environment.getType(parser, typeName);
		if (type == null) throw new ScriptParsingException("Unknown type: " + typeName, parser.input);
		parser.input.expectAfterWhitespace(')');
		return type;
	}

	public @Nullable InsnTree nextElse(ExpressionParser parser) throws ScriptParsingException {
		return parser.input.hasIdentifierAfterWhitespace("else") ? parser.nextCompoundExpression() : null;
	}
}