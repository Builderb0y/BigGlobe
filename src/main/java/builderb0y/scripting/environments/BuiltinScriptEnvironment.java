package builderb0y.scripting.environments;

import java.lang.invoke.StringConcatFactory;
import java.util.Arrays;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarePostAssignInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.flow.loop.WhileInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.BreakInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ContinueInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MemberKeywordHandler;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.SpecialFunctionSyntax;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.*;
import builderb0y.scripting.parsing.UserClassDefiner;
import builderb0y.scripting.util.PrintSink;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BuiltinScriptEnvironment {

	public static PrintSink PRINTER = PrintSink.forPrintStream(System.out);

	public static final MethodInfo
		STRING_CONCAT_FACTORY      = MethodInfo.getMethod(StringConcatFactory.class, "makeConcat"),
		PRINTLN_BOOLEAN            = MethodInfo.findMethod(PrintSink.class, "println", void.class, boolean.class),
		PRINTLN_CHAR               = MethodInfo.findMethod(PrintSink.class, "println", void.class,    char.class),
		PRINTLN_INT                = MethodInfo.findMethod(PrintSink.class, "println", void.class,     int.class),
		PRINTLN_LONG               = MethodInfo.findMethod(PrintSink.class, "println", void.class,    long.class),
		PRINTLN_FLOAT              = MethodInfo.findMethod(PrintSink.class, "println", void.class,   float.class),
		PRINTLN_DOUBLE             = MethodInfo.findMethod(PrintSink.class, "println", void.class,  double.class),
		PRINTLN_STRING             = MethodInfo.findMethod(PrintSink.class, "println", void.class,  String.class),
		PRINTLN_OBJECT             = MethodInfo.findMethod(PrintSink.class, "println", void.class,  Object.class);
	public static final FieldInfo
		PRINTER_FIELD = FieldInfo.getField(BuiltinScriptEnvironment.class, "PRINTER");

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()

		//////////////// variables ////////////////

		.addVariable("true",  ldc(true))
		.addVariable("yes",   ldc(true))
		.addVariable("false", ldc(false))
		.addVariable("no",    ldc(false))
		.addVariable("noop",  noop)
		.addVariable("null",  ldc(null, TypeInfos.OBJECT.generic()))

		//////////////// types ////////////////

		.addType("boolean",    TypeInfos.BOOLEAN)
		.addType("byte",       TypeInfos.   BYTE)
		.addType("short",      TypeInfos.  SHORT)
		.addType("int",        TypeInfos.    INT)
		.addType("long",       TypeInfos.   LONG)
		.addType("float",      TypeInfos.  FLOAT)
		.addType("double",     TypeInfos. DOUBLE)
		.addType("char",       TypeInfos.   CHAR)
		.addType("void",       TypeInfos.   VOID)

		.addType("Boolean",    TypeInfos.BOOLEAN_WRAPPER)
		.addType("Byte",       TypeInfos.   BYTE_WRAPPER)
		.addType("Short",      TypeInfos.  SHORT_WRAPPER)
		.addType("Integer",    TypeInfos.    INT_WRAPPER)
		.addType("Long",       TypeInfos.   LONG_WRAPPER)
		.addType("Float",      TypeInfos.  FLOAT_WRAPPER)
		.addType("Double",     TypeInfos. DOUBLE_WRAPPER)
		.addType("Character",  TypeInfos.   CHAR_WRAPPER)
		.addType("Void",       TypeInfos.   VOID_WRAPPER)
		.addType("Number",     TypeInfos.NUMBER)

		.addType("Object",     TypeInfos.OBJECT)
		.addType("Comparable", TypeInfos.COMPARABLE)
		.addType("String",     TypeInfos.STRING)
		//.addType("Throwable",  TypeInfos.THROWABLE)
		.addType("Class",      TypeInfos.CLASS)

		//////////////// functions ////////////////

		.addFunction("return", (ExpressionParser parser, String name, InsnTree... arguments) -> {
			return new CastResult(
				parser.createReturn(
					switch (arguments.length) {
						case 0 -> noop;
						case 1 -> arguments[0];
						default -> throw new ScriptParsingException("Returning multiple values is not supported", parser.input);
					}
				),
				false
			);
		})
		/*
		.addFunction("throw", (parser, name, arguments) -> {
			InsnTree toThrow = ScriptEnvironment.castArgument(parser, name, TypeInfos.THROWABLE, CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(throw_(toThrow), toThrow != arguments[0]);
		})
		*/
		.addFunction("print", makePrint())

		//////////////// keywords ////////////////

		.addKeyword("var", makeVar())
		.addKeyword("class", makeClass())
		.addKeyword("if", (ExpressionParser parser, String name) -> nextIfElse(parser, false))
		.addKeyword("unless", (ExpressionParser parser, String name) -> nextIfElse(parser, true))
		.addMemberKeyword(TypeInfos.BOOLEAN, "if", (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			return nextIfElse(receiver, parser, false);
		})
		.addMemberKeyword(TypeInfos.BOOLEAN, "unless", (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			return nextIfElse(receiver, parser, true);
		})
		.addKeyword("while", (ExpressionParser parser, String name) -> {
			LoopName loopName = LoopName.of(parser.input.readIdentifierOrNullAfterWhitespace());
			ConditionBody whileStatement = ConditionBody.parse(parser);
			return while_(loopName, whileStatement.condition(), whileStatement.body());
		})
		.addKeyword("until", (ExpressionParser parser, String name) -> {
			LoopName loopName = LoopName.of(parser.input.readIdentifierOrNullAfterWhitespace());
			ConditionBody whileStatement = ConditionBody.parse(parser);
			return while_(loopName, not(whileStatement.condition()), whileStatement.body());
		})
		.addKeyword("do", (ExpressionParser parser, String name) -> switch (parser.input.readIdentifierAfterWhitespace()) {
			case "while" -> {
				LoopName loopName = LoopName.of(parser.input.readIdentifierOrNullAfterWhitespace());
				ConditionBody whileStatement = ConditionBody.parse(parser);
				yield doWhile(parser, loopName, whileStatement.condition(), whileStatement.body());
			}
			case "until" -> {
				LoopName loopName = LoopName.of(parser.input.readIdentifierOrNullAfterWhitespace());
				ConditionBody whileStatement = ConditionBody.parse(parser);
				yield doWhile(parser, loopName, not(whileStatement.condition()), whileStatement.body());
			}
			default -> throw new ScriptParsingException("Expected 'while' or 'until' after 'do'", parser.input);
		})
		.addKeyword("repeat", (ExpressionParser parser, String name) -> {
			LoopName loopName = LoopName.of(parser.input.readIdentifierOrNullAfterWhitespace());
			ScriptBody repeatStatement = ScriptBody.parse(parser, (InsnTree count, ExpressionParser parser1) -> count.cast(parser1, TypeInfos.INT, CastMode.IMPLICIT_THROW));
			return WhileInsnTree.createRepeat(parser, loopName, repeatStatement.expression(), repeatStatement.body());
		})
		.addKeyword("for", (ExpressionParser parser, String name) -> {
			return ForLoop.parse(parser);
		})
		.addKeyword("switch", (ExpressionParser parser, String name) -> {
			SwitchBody switchBody = SwitchBody.parse(parser);
			return switchBody.maybeWrap(switch_(parser, switchBody.value(), switchBody.cases()));
		})
		.addKeyword("block", (ExpressionParser parser, String name) -> {
			LoopName loopName = LoopName.of(parser.input.readIdentifierOrNullAfterWhitespace());
			return block(loopName, ParenthesizedScript.parse(parser).contents());
		})
		.addKeyword("break", (ExpressionParser parser, String name) -> {
			parser.input.expectAfterWhitespace('(');
			String loopName = parser.input.readIdentifierOrNullAfterWhitespace();
			parser.input.expectAfterWhitespace(')');
			return new BreakInsnTree(loopName);
		})
		.addKeyword("continue", (ExpressionParser parser, String name) -> {
			parser.input.expectAfterWhitespace('(');
			String loopName = parser.input.readIdentifierOrNullAfterWhitespace();
			parser.input.expectAfterWhitespace(')');
			return new ContinueInsnTree(loopName);
		})
		.addKeyword("compare", (ExpressionParser parser, String name) -> {
			return SpecialFunctionSyntax.Compare.parse(parser).buildInsnTree();
		})
		.addKeyword("noscope", (ExpressionParser parser, String name) -> {
			parser.input.expectAfterWhitespace('(');
			InsnTree tree = parser.nextScript();
			parser.input.expectAfterWhitespace(')');
			return tree;
		})

		//////////////// member keywords ////////////////

		.addMemberKeyword(TypeInfos.OBJECT, "is", (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			TypeInfo type = nextParenthesizedType(parser);
			if (type.isPrimitive()) {
				throw new ScriptParsingException("Can't check object.is(primitive)", parser.input);
			}
			return instanceOf(receiver, type);
		})
		.addMemberKeyword(TypeInfos.OBJECT, "isnt", (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			TypeInfo type = nextParenthesizedType(parser);
			if (type.isPrimitive()) {
				throw new ScriptParsingException("Can't check object.isnt(primitive)", parser.input);
			}
			return not(parser, instanceOf(receiver, type));
		})
		.addMemberKeyword(null, "as", (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			return receiver.cast(parser, nextParenthesizedType(parser), CastMode.EXPLICIT_THROW);
		})
		.addMemberKeyword(TypeInfos.BYTE,   "isBetween", makeBetween())
		.addMemberKeyword(TypeInfos.SHORT,  "isBetween", makeBetween())
		.addMemberKeyword(TypeInfos.INT,    "isBetween", makeBetween())
		.addMemberKeyword(TypeInfos.LONG,   "isBetween", makeBetween())
		.addMemberKeyword(TypeInfos.FLOAT,  "isBetween", makeBetween())
		.addMemberKeyword(TypeInfos.DOUBLE, "isBetween", makeBetween())
		.addMemberKeyword(TypeInfos.CHAR,   "isBetween", makeBetween())

		//////////////// casting ////////////////

		//boolean
		.addCastIdentity(TypeInfos.BOOLEAN, TypeInfos.BYTE, false)
		.addCastIdentity(TypeInfos.BOOLEAN, TypeInfos.CHAR, false)
		.addCastIdentity(TypeInfos.BOOLEAN, TypeInfos.SHORT, false)
		.addCastIdentity(TypeInfos.BOOLEAN, TypeInfos.INT, false)
		.addCast(TypeInfos.BOOLEAN, TypeInfos.LONG, false, CastingSupport.I2L.changeInput(TypeInfos.BOOLEAN))
		.addCast(TypeInfos.BOOLEAN, TypeInfos.FLOAT, false, CastingSupport.I2F.changeInput(TypeInfos.BOOLEAN))
		.addCast(TypeInfos.BOOLEAN, TypeInfos.DOUBLE, false, CastingSupport.I2D.changeInput(TypeInfos.BOOLEAN))
		//byte
		//.addCast(TypeInfos.BYTE, TypeInfos.BOOLEAN, false, CastingSupport.I2Z)
		.addCastIdentity(TypeInfos.BYTE, TypeInfos.CHAR, true)
		.addCastIdentity(TypeInfos.BYTE, TypeInfos.SHORT, true)
		.addCastIdentity(TypeInfos.BYTE, TypeInfos.INT, true)
		.addCast(TypeInfos.BYTE, TypeInfos.LONG, true, CastingSupport.I2L)
		.addCast(TypeInfos.BYTE, TypeInfos.FLOAT, true, CastingSupport.I2F)
		.addCast(TypeInfos.BYTE, TypeInfos.DOUBLE, true, CastingSupport.I2D)
		//char
		//.addCast(TypeInfos.CHAR, TypeInfos.BOOLEAN, false, CastingSupport.I2Z)
		.addCast(TypeInfos.CHAR, TypeInfos.BYTE, false, CastingSupport.I2B)
		.addCast(TypeInfos.CHAR, TypeInfos.SHORT, true, CastingSupport.I2S)
		.addCastIdentity(TypeInfos.CHAR, TypeInfos.INT, true)
		.addCast(TypeInfos.CHAR, TypeInfos.LONG, true, CastingSupport.I2L)
		.addCast(TypeInfos.CHAR, TypeInfos.FLOAT, true, CastingSupport.I2F)
		.addCast(TypeInfos.CHAR, TypeInfos.DOUBLE, true, CastingSupport.I2D)
		//short
		//.addCast(TypeInfos.SHORT, TypeInfos.BOOLEAN, false, CastingSupport.I2Z)
		.addCast(TypeInfos.SHORT, TypeInfos.BYTE, false, CastingSupport.I2B)
		.addCast(TypeInfos.SHORT, TypeInfos.CHAR, false, CastingSupport.I2C)
		.addCastIdentity(TypeInfos.SHORT, TypeInfos.INT, true)
		.addCast(TypeInfos.SHORT, TypeInfos.LONG, true, CastingSupport.I2L)
		.addCast(TypeInfos.SHORT, TypeInfos.FLOAT, true, CastingSupport.I2F)
		.addCast(TypeInfos.SHORT, TypeInfos.DOUBLE, true, CastingSupport.I2D)
		//int
		//.addCast(CastingSupport.I2Z)
		.addCast(CastingSupport.I2B)
		.addCast(CastingSupport.I2C)
		.addCast(CastingSupport.I2S)
		.addCast(CastingSupport.I2L)
		.addCast(CastingSupport.I2F)
		.addCast(CastingSupport.I2D)
		//long
		//.addCast(CastingSupport.L2Z)
		.addCasts(CastingSupport.L2I, CastingSupport.I2B)
		.addCasts(CastingSupport.L2I, CastingSupport.I2C)
		.addCasts(CastingSupport.L2I, CastingSupport.I2S)
		.addCast(CastingSupport.L2I)
		.addCast(CastingSupport.L2F)
		.addCast(CastingSupport.L2D)
		//float
		.addCast(TypeInfos.FLOAT, TypeInfos.BOOLEAN, false, CastingSupport.F2Z)
		.addCasts(CastingSupport.F2I, CastingSupport.I2B)
		.addCasts(CastingSupport.F2I, CastingSupport.I2C)
		.addCasts(CastingSupport.F2I, CastingSupport.I2S)
		.addCast(CastingSupport.F2I)
		.addCast(CastingSupport.F2L)
		.addCast(CastingSupport.F2D)
		//double
		.addCast(TypeInfos.DOUBLE, TypeInfos.BOOLEAN, false, CastingSupport.D2Z)
		.addCasts(CastingSupport.D2I, CastingSupport.I2B)
		.addCasts(CastingSupport.D2I, CastingSupport.I2C)
		.addCasts(CastingSupport.D2I, CastingSupport.I2S)
		.addCast(CastingSupport.D2I)
		.addCast(CastingSupport.D2L)
		.addCast(CastingSupport.D2F)
		//boxing
		.addCastConstant(CastingSupport.   BYTE_CONSTANT_FACTORY, true)
		.addCastConstant(CastingSupport.  SHORT_CONSTANT_FACTORY, true)
		.addCastConstant(CastingSupport.    INT_CONSTANT_FACTORY, true)
		.addCastConstant(CastingSupport.   LONG_CONSTANT_FACTORY, true)
		.addCastConstant(CastingSupport.  FLOAT_CONSTANT_FACTORY, true)
		.addCastConstant(CastingSupport. DOUBLE_CONSTANT_FACTORY, true)
		.addCastConstant(CastingSupport.   CHAR_CONSTANT_FACTORY, true)
		.addCastConstant(CastingSupport.BOOLEAN_CONSTANT_FACTORY, true)
		/*
		.addCastInvokeStatic(Byte     .class, "valueOf", true, Byte     .class, byte     .class)
		.addCastInvokeStatic(Short    .class, "valueOf", true, Short    .class, short    .class)
		.addCastInvokeStatic(Integer  .class, "valueOf", true, Integer  .class, int      .class)
		.addCastInvokeStatic(Long     .class, "valueOf", true, Long     .class, long     .class)
		.addCastInvokeStatic(Float    .class, "valueOf", true, Float    .class, float    .class)
		.addCastInvokeStatic(Double   .class, "valueOf", true, Double   .class, double   .class)
		.addCastInvokeStatic(Character.class, "valueOf", true, Character.class, char     .class)
		.addCastInvokeStatic(Boolean  .class, "valueOf", true, Boolean  .class, boolean  .class)
		*/
		//unboxing
		.addCast(TypeInfos.BYTE_WRAPPER,    TypeInfos.BYTE,            true, CastingSupport.invokeVirtual(MethodInfo.getMethod(Byte     .class,    "byteValue")))
		.addCast(TypeInfos.SHORT_WRAPPER,   TypeInfos.SHORT,           true, CastingSupport.invokeVirtual(MethodInfo.getMethod(Short    .class,   "shortValue")))
		.addCast(TypeInfos.INT_WRAPPER,     TypeInfos.INT,             true, CastingSupport.invokeVirtual(MethodInfo.getMethod(Integer  .class,     "intValue")))
		.addCast(TypeInfos.LONG_WRAPPER,    TypeInfos.LONG,            true, CastingSupport.invokeVirtual(MethodInfo.getMethod(Long     .class,    "longValue")))
		.addCast(TypeInfos.FLOAT_WRAPPER,   TypeInfos.FLOAT,           true, CastingSupport.invokeVirtual(MethodInfo.getMethod(Float    .class,   "floatValue")))
		.addCast(TypeInfos.DOUBLE_WRAPPER,  TypeInfos.DOUBLE,          true, CastingSupport.invokeVirtual(MethodInfo.getMethod(Double   .class,  "doubleValue")))
		.addCast(TypeInfos.CHAR_WRAPPER,    TypeInfos.CHAR,            true, CastingSupport.invokeVirtual(MethodInfo.getMethod(Character.class,    "charValue")))
		.addCast(TypeInfos.BOOLEAN_WRAPPER, TypeInfos.BOOLEAN,         true, CastingSupport.invokeVirtual(MethodInfo.getMethod(Boolean  .class, "booleanValue")))
		//toString
		.addCast(TypeInfos.BYTE,    TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Byte     .class, "toString", String.class, byte   .class)))
		.addCast(TypeInfos.SHORT,   TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Short    .class, "toString", String.class, short  .class)))
		.addCast(TypeInfos.INT,     TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Integer  .class, "toString", String.class, int    .class)))
		.addCast(TypeInfos.LONG,    TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Long     .class, "toString", String.class, long   .class)))
		.addCast(TypeInfos.FLOAT,   TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Float    .class, "toString", String.class, float  .class)))
		.addCast(TypeInfos.DOUBLE,  TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Double   .class, "toString", String.class, double .class)))
		.addCast(TypeInfos.CHAR,    TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Character.class, "toString", String.class, char   .class)))
		.addCast(TypeInfos.BOOLEAN, TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Boolean  .class, "toString", String.class, boolean.class)))
		.addCast(TypeInfos.OBJECT,  TypeInfos.STRING, true, CastingSupport.invokeStatic(MethodInfo.findMethod(Objects  .class, "toString", String.class, Object .class)))

		//////////////// casting with round mode ////////////////

		.addFunctionMultiInvokeStatics(CastingSupport.class, "floorInt", "ceilInt", "floorLong", "ceilLong", "roundInt", "roundLong")
		.addFunction("truncInt", makeOpcode("truncInt(float value)", TypeInfos.FLOAT, TypeInfos.INT, F2I))
		.addFunction("truncInt", makeOpcode("truncInt(double value)", TypeInfos.DOUBLE, TypeInfos.INT, D2I))
		.addFunction("truncLong", makeOpcode("truncLong(float value)", TypeInfos.FLOAT, TypeInfos.LONG, F2L))
		.addFunction("truncLong", makeOpcode("truncLong(double value)", TypeInfos.DOUBLE, TypeInfos.LONG, D2L))
	);

	public static FunctionHandler makeOpcode(String name, TypeInfo from, TypeInfo to, int opcode) {
		return new FunctionHandler.Named(name, (ExpressionParser parser, String name1, InsnTree... arguments) -> {
			if (arguments.length == 1 && arguments[0].getTypeInfo().equals(from)) {
				return new CastResult(new OpcodeCastInsnTree(arguments[0], opcode, to), false);
			}
			return null;
		});
	}

	public static TypeInfo nextParenthesizedType(ExpressionParser parser) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		String typeName = parser.input.expectIdentifierAfterWhitespace();
		TypeInfo type = parser.environment.getType(parser, typeName);
		if (type == null) throw new ScriptParsingException("Unknown type: " + typeName, parser.input);
		parser.input.expectAfterWhitespace(')');
		return type;
	}

	public static FunctionHandler makePrint() {
		return new FunctionHandler.Named("builtin function print(anything)", (ExpressionParser parser, String name, InsnTree... arguments) -> {
			if (arguments.length == 0) {
				throw new ScriptParsingException("Not allowed to print nothing", parser.input);
			}
			InsnTree loadOut = getStatic(PRINTER_FIELD);
			if (arguments.length == 1) {
				return new CastResult(
					switch (arguments[0].getTypeInfo().getSort()) {
						case VOID          -> throw new ScriptParsingException("Attempt to print void", parser.input);
						case BYTE, SHORT   -> invokeInstance(loadOut, PRINTLN_INT,     arguments[0].cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW));
						case INT           -> invokeInstance(loadOut, PRINTLN_INT,     arguments);
						case LONG          -> invokeInstance(loadOut, PRINTLN_LONG,    arguments);
						case FLOAT         -> invokeInstance(loadOut, PRINTLN_FLOAT,   arguments);
						case DOUBLE        -> invokeInstance(loadOut, PRINTLN_DOUBLE,  arguments);
						case CHAR          -> invokeInstance(loadOut, PRINTLN_CHAR,    arguments);
						case BOOLEAN       -> invokeInstance(loadOut, PRINTLN_BOOLEAN, arguments);
						case ARRAY, OBJECT -> invokeInstance(loadOut, PRINTLN_OBJECT,  arguments);
					},
					false
				);
			}
			else {
				return new CastResult(
					invokeInstance(
						loadOut,
						PRINTLN_STRING,
						invokeDynamic(
							STRING_CONCAT_FACTORY,
							new MethodInfo(
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
						)
					),
					false
				);
			}
		});
	}

	public static KeywordHandler makeVar() {
		return (ExpressionParser parser, String name) -> {
			String varName = parser.verifyName(parser.input.expectIdentifierAfterWhitespace(), "variable");
			boolean reuse;
			if (parser.input.hasOperatorAfterWhitespace("=")) reuse = false;
			else if (parser.input.hasOperatorAfterWhitespace(":=")) reuse = true;
			else throw new ScriptParsingException("Expected '=' or ':='", parser.input);
			parser.environment.user().reserveVariable(varName);
			InsnTree initializer = parser.nextSingleExpression();
			parser.environment.user().setVariableType(varName, initializer.getTypeInfo());
			parser.environment.user().assignVariable(varName);
			LazyVarInfo variable = new LazyVarInfo(varName, initializer.getTypeInfo());
			return (
				reuse
				? new VariableDeclarePostAssignInsnTree(variable, initializer)
				: new VariableDeclareAssignInsnTree(variable, initializer)
			);
		};
	}

	public static KeywordHandler makeClass() {
		return (ExpressionParser parser, String name) -> {
			String className = parser.verifyName(parser.input.expectIdentifierAfterWhitespace(), "class");
			parser.checkType(className);
			new UserClassDefiner(parser, className).parse();
			return noop;
		};
	}

	public static MemberKeywordHandler makeBetween() {
		return (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			return switch (mode) {
				case NORMAL, NULLABLE -> {
					yield SpecialFunctionSyntax.IsBetween.parse(parser, receiver).toTree(parser);
				}
				case RECEIVER, NULLABLE_RECEIVER -> {
					throw new ScriptParsingException("Can't use isBetween() with nullable syntax.", parser.input);
				}
			};
		};
	}

	public static InsnTree nextIfElse(ExpressionParser parser, boolean negate) throws ScriptParsingException {
		ConditionBody ifStatement = ConditionBody.parse(parser);
		InsnTree elseStatement = nextElse(parser);
		ConditionTree condition = ifStatement.condition();
		if (negate) condition = not(condition);
		return (
			elseStatement != null
			? ifElse(
				parser,
				condition,
				ifStatement.body(),
				elseStatement
			)
			: ifThen(
				condition,
				ifStatement.body()
			)
		);
	}

	public static InsnTree nextIfElse(InsnTree condition, ExpressionParser parser, boolean negate) throws ScriptParsingException {
		ConditionTree conditionTree = condition(parser, condition);
		if (negate) conditionTree = not(conditionTree);
		InsnTree ifStatement = seq(tryParenthesized(parser), ldc(!negate));
		InsnTree elseStatement = nextElse(parser);
		elseStatement = elseStatement != null ? seq(elseStatement, ldc(negate)) : ldc(negate);
		return ifElse(parser, conditionTree, ifStatement, elseStatement);
	}

	public static @Nullable InsnTree nextElse(ExpressionParser parser) throws ScriptParsingException {
		return parser.input.hasIdentifierAfterWhitespace("else") ? tryParenthesized(parser) : null;
	}

	public static InsnTree tryParenthesized(ExpressionParser parser) throws ScriptParsingException {
		return parser.input.peekAfterWhitespace() == '(' ? ParenthesizedScript.parse(parser).contents() : parser.nextSingleExpression();
	}
}