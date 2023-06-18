package builderb0y.scripting.environments;

import java.io.PrintStream;
import java.lang.invoke.StringConcatFactory;
import java.util.Arrays;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.flow.WhileInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.BreakInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ContinueInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.SpecialFunctionSyntax;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.*;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BuiltinScriptEnvironment {

	public static final MethodInfo
		STRING_CONCAT_FACTORY = MethodInfo.getMethod(StringConcatFactory.class, "makeConcat"),
		PRINTLN = MethodInfo.findMethod(PrintStream.class, "println", void.class, String.class);
	public static final FieldInfo
		SYSTEM_OUT = FieldInfo.getField(System.class, "out");

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()

		//////////////// variables ////////////////

		.addVariable("true",  ldc(true))
		.addVariable("yes",   ldc(true))
		.addVariable("false", ldc(false))
		.addVariable("no",    ldc(false))
		.addVariable("noop",  noop)
		.addVariable("null",  ldc(null, TypeInfos.OBJECT))

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
		.addType("Throwable",  TypeInfos.THROWABLE)
		.addType("Class",      TypeInfos.CLASS)

		//////////////// functions ////////////////

		.addFunction("return", (parser, name, arguments) -> {
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
		.addFunction("print", (parser, name, arguments) -> {
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
			return new CastResult(invokeInstance(loadOut, PRINTLN, concat), false);
		})

		//////////////// keywords ////////////////

		.addKeyword("if", (parser, name) -> nextIfElse(parser, false))
		.addKeyword("unless", (parser, name) -> nextIfElse(parser, true))
		.addMemberKeyword(TypeInfos.BOOLEAN, "if", (parser, receiver, name) -> nextIfElse(receiver, parser, false))
		.addMemberKeyword(TypeInfos.BOOLEAN, "unless", (parser, receiver, name) -> nextIfElse(receiver, parser, true))
		.addKeyword("while", (parser, name) -> {
			ConditionBody whileStatement = ConditionBody.parse(parser);
			return while_(whileStatement.condition(), whileStatement.body());
		})
		.addKeyword("until", (parser, name) -> {
			ConditionBody whileStatement = ConditionBody.parse(parser);
			return while_(not(whileStatement.condition()), whileStatement.body());
		})
		.addKeyword("do", (parser, name) -> switch (parser.input.readIdentifierAfterWhitespace()) {
			case "while" -> {
				ConditionBody whileStatement = ConditionBody.parse(parser);
				yield doWhile(parser, whileStatement.condition(), whileStatement.body());
			}
			case "until" -> {
				ConditionBody whileStatement = ConditionBody.parse(parser);
				yield doWhile(parser, not(whileStatement.condition()), whileStatement.body());
			}
			default -> throw new ScriptParsingException("Expected 'while' or 'until' after 'do'", parser.input);
		})
		.addKeyword("repeat", (parser, name) -> {
			ScriptBody repeatStatement = ScriptBody.parse(parser, (count, parser1) -> count.cast(parser1, TypeInfos.INT, CastMode.IMPLICIT_THROW));
			return WhileInsnTree.createRepeat(parser, repeatStatement.expression(), repeatStatement.body());
		})
		.addKeyword("for", (parser, name) -> {
			ForEachLoop enhancedLoop = ForEachLoop.tryParse(parser);
			if (enhancedLoop != null) {
				return enhancedLoop.toLoop(parser);
			}
			else {
				ForLoop loop = ForLoop.parse(parser);
				return for_(loop.initializer(), loop.condition(), loop.step(), loop.body());
			}
		})
		.addKeyword("switch", (parser, name) -> {
			SwitchBody switchBody = SwitchBody.parse(parser);
			return switchBody.maybeWrap(switch_(parser, switchBody.value(), switchBody.cases()));
		})
		.addKeyword("block", (parser, name) -> {
			return block(ParenthesizedScript.parse(parser).contents());
		})
		.addKeyword("break", (parser, name) -> {
			parser.input.expectAfterWhitespace('(');
			parser.input.expectAfterWhitespace(')');
			return BreakInsnTree.INSTANCE;
		})
		.addKeyword("continue", (parser, name) -> {
			parser.input.expectAfterWhitespace('(');
			parser.input.expectAfterWhitespace(')');
			return ContinueInsnTree.INSTANCE;
		})
		.addKeyword("compare", (parser, name) -> {
			return SpecialFunctionSyntax.Compare.parse(parser).buildInsnTree();
		})

		//////////////// member keywords ////////////////

		.addMemberKeyword(TypeInfos.OBJECT, "is", (parser, receiver, name) -> {
			TypeInfo type = nextParenthesizedType(parser);
			if (type.isPrimitive()) {
				throw new ScriptParsingException("Can't check object.is(primitive)", parser.input);
			}
			return instanceOf(receiver, type);
		})
		.addMemberKeyword(TypeInfos.OBJECT, "isnt", (parser, receiver, name) -> {
			TypeInfo type = nextParenthesizedType(parser);
			if (type.isPrimitive()) {
				throw new ScriptParsingException("Can't check object.isnt(primitive)", parser.input);
			}
			return not(parser, instanceOf(receiver, type));
		})
		.addMemberKeyword(null, "as", (parser, receiver, name) -> {
			return receiver.cast(parser, nextParenthesizedType(parser), CastMode.EXPLICIT_THROW);
		})

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
		.addCastInvokeStatic(Byte     .class, "valueOf", true, Byte     .class, byte     .class)
		.addCastInvokeStatic(Short    .class, "valueOf", true, Short    .class, short    .class)
		.addCastInvokeStatic(Integer  .class, "valueOf", true, Integer  .class, int      .class)
		.addCastInvokeStatic(Long     .class, "valueOf", true, Long     .class, long     .class)
		.addCastInvokeStatic(Float    .class, "valueOf", true, Float    .class, float    .class)
		.addCastInvokeStatic(Double   .class, "valueOf", true, Double   .class, double   .class)
		.addCastInvokeStatic(Character.class, "valueOf", true, Character.class, char     .class)
		.addCastInvokeStatic(Boolean  .class, "valueOf", true, Boolean  .class, boolean  .class)
		//unboxing
		.addCast(TypeInfos.BYTE_WRAPPER,    TypeInfos.BYTE,            true, CastingSupport.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.   BYTE_WRAPPER, "byteValue",    TypeInfos.BYTE)))
		.addCast(TypeInfos.SHORT_WRAPPER,   TypeInfos.SHORT,           true, CastingSupport.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.  SHORT_WRAPPER, "shortValue",   TypeInfos.SHORT)))
		.addCast(TypeInfos.INT_WRAPPER,     TypeInfos.INT,             true, CastingSupport.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.    INT_WRAPPER, "intValue",     TypeInfos.INT)))
		.addCast(TypeInfos.LONG_WRAPPER,    TypeInfos.LONG,            true, CastingSupport.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.   LONG_WRAPPER, "longValue",    TypeInfos.LONG)))
		.addCast(TypeInfos.FLOAT_WRAPPER,   TypeInfos.FLOAT,           true, CastingSupport.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.  FLOAT_WRAPPER, "floatValue",   TypeInfos.FLOAT)))
		.addCast(TypeInfos.DOUBLE_WRAPPER,  TypeInfos.DOUBLE,          true, CastingSupport.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos. DOUBLE_WRAPPER, "doubleValue",  TypeInfos.DOUBLE)))
		.addCast(TypeInfos.CHAR_WRAPPER,    TypeInfos.CHAR,            true, CastingSupport.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.   CHAR_WRAPPER, "charValue",    TypeInfos.CHAR)))
		.addCast(TypeInfos.BOOLEAN_WRAPPER, TypeInfos.BOOLEAN,         true, CastingSupport.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.BOOLEAN_WRAPPER, "booleanValue", TypeInfos.BOOLEAN)))
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
		return new FunctionHandler.Named(name, (parser, name1, arguments) -> {
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