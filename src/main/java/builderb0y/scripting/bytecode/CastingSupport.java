package builderb0y.scripting.bytecode;

import java.lang.invoke.MethodHandles;

import org.objectweb.asm.Opcodes;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.D2ZInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.F2ZInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastHandlerData;
import builderb0y.scripting.environments.MutableScriptEnvironment.MultiCastHandler;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class CastingSupport {

	public static final CastHandlerData
		I2B = data(TypeInfos.INT,    TypeInfos.BYTE,    false, opcode(Opcodes.I2B)),
		I2S = data(TypeInfos.INT,    TypeInfos.SHORT,   false, opcode(Opcodes.I2S)),
		I2C = data(TypeInfos.INT,    TypeInfos.CHAR,    false, opcode(Opcodes.I2C)),
		I2L = data(TypeInfos.INT,    TypeInfos.LONG,    true,  opcode(Opcodes.I2L)),
		I2F = data(TypeInfos.INT,    TypeInfos.FLOAT,   true,  opcode(Opcodes.I2F)),
		I2D = data(TypeInfos.INT,    TypeInfos.DOUBLE,  true,  opcode(Opcodes.I2D)),
		L2I = data(TypeInfos.LONG,   TypeInfos.INT,     false, opcode(Opcodes.L2I)),
		L2F = data(TypeInfos.LONG,   TypeInfos.FLOAT,   true,  opcode(Opcodes.L2F)),
		L2D = data(TypeInfos.LONG,   TypeInfos.DOUBLE,  true,  opcode(Opcodes.L2D)),
		F2I = data(TypeInfos.FLOAT,  TypeInfos.INT,     false, invokeStatic(MethodInfo.findMethod(CastingSupport.class, "floorInt", int.class, float.class))),
		F2L = data(TypeInfos.FLOAT,  TypeInfos.LONG,    false, invokeStatic(MethodInfo.findMethod(CastingSupport.class, "floorLong", long.class, float.class))),
		F2D = data(TypeInfos.FLOAT,  TypeInfos.DOUBLE,  true,  opcode(Opcodes.F2D)),
		D2I = data(TypeInfos.DOUBLE, TypeInfos.INT,     false, invokeStatic(MethodInfo.findMethod(CastingSupport.class, "floorInt", int.class, double.class))),
		D2L = data(TypeInfos.DOUBLE, TypeInfos.LONG,    false, invokeStatic(MethodInfo.findMethod(CastingSupport.class, "floorLong", long.class, double.class))),
		D2F = data(TypeInfos.DOUBLE, TypeInfos.FLOAT,   false, opcode(Opcodes.D2F)),
		F2Z = data(TypeInfos.FLOAT,  TypeInfos.BOOLEAN, false, (parser, value, to, implicit) -> new F2ZInsnTree(value)),
		D2Z = data(TypeInfos.DOUBLE, TypeInfos.BOOLEAN, false, (parser, value, to, implicit) -> new D2ZInsnTree(value));
	public static final FieldInfo
		TRUE_FIELD  = FieldInfo.getField(Boolean.class, "TRUE" ),
		FALSE_FIELD = FieldInfo.getField(Boolean.class, "FALSE");
	public static final MethodInfo
		BOOLEAN_VALUE_OF = MethodInfo.findMethod(Boolean.class, "valueOf", Boolean.class, boolean.class);
	public static final AbstractConstantFactory
		BYTE_CONSTANT_FACTORY    = new ConstantFactory(MethodInfo.getMethod(CastingSupport.class, "makeByte"   ), MethodInfo.findMethod(Byte     .class, "valueOf", Byte     .class, byte   .class), TypeInfos.BYTE,    TypeInfos.BYTE_WRAPPER   ),
		SHORT_CONSTANT_FACTORY   = new ConstantFactory(MethodInfo.getMethod(CastingSupport.class, "makeShort"  ), MethodInfo.findMethod(Short    .class, "valueOf", Short    .class, short  .class), TypeInfos.SHORT,   TypeInfos.SHORT_WRAPPER  ),
		INT_CONSTANT_FACTORY     = new ConstantFactory(MethodInfo.getMethod(CastingSupport.class, "makeInt"    ), MethodInfo.findMethod(Integer  .class, "valueOf", Integer  .class, int    .class), TypeInfos.INT,     TypeInfos.INT_WRAPPER    ),
		LONG_CONSTANT_FACTORY    = new ConstantFactory(MethodInfo.getMethod(CastingSupport.class, "makeLong"   ), MethodInfo.findMethod(Long     .class, "valueOf", Long     .class, long   .class), TypeInfos.LONG,    TypeInfos.LONG_WRAPPER   ),
		FLOAT_CONSTANT_FACTORY   = new ConstantFactory(MethodInfo.getMethod(CastingSupport.class, "makeFloat"  ), MethodInfo.findMethod(Float    .class, "valueOf", Float    .class, float  .class), TypeInfos.FLOAT,   TypeInfos.FLOAT_WRAPPER  ),
		DOUBLE_CONSTANT_FACTORY  = new ConstantFactory(MethodInfo.getMethod(CastingSupport.class, "makeDouble" ), MethodInfo.findMethod(Double   .class, "valueOf", Double   .class, double .class), TypeInfos.DOUBLE,  TypeInfos.DOUBLE_WRAPPER ),
		CHAR_CONSTANT_FACTORY    = new ConstantFactory(MethodInfo.getMethod(CastingSupport.class, "makeChar"   ), MethodInfo.findMethod(Character.class, "valueOf", Character.class, char   .class), TypeInfos.CHAR,    TypeInfos.CHAR_WRAPPER   ),
		BOOLEAN_CONSTANT_FACTORY = new AbstractConstantFactory(TypeInfos.BOOLEAN, TypeInfos.BOOLEAN_WRAPPER) {

			@Override
			public InsnTree createConstant(ConstantValue constant) {
				return getStatic(constant.asBoolean() ? TRUE_FIELD : FALSE_FIELD);
			}

			@Override
			public InsnTree createNonConstant(InsnTree tree) {
				return InsnTrees.invokeStatic(BOOLEAN_VALUE_OF, tree);
			}
		};

	public static InsnTree primitiveCast(InsnTree value, TypeInfo type) {
		if (value.getTypeInfo().equals(type)) {
			return value;
		}
		//passing in a null parser is NOT recommended, but in this case it is safe.
		InsnTree casted = BuiltinScriptEnvironment.INSTANCE.cast(null, value, type, false);
		if (casted != null) return casted;
		else throw new IllegalArgumentException("Can't primitively cast " + value.describe() + " to " + type);
	}

	public static CastHandler opcode(int opcode) {
		return (parser, value, to, implicit) -> new OpcodeCastInsnTree(value, opcode, to);
	}

	public static CastHandler invokeVirtual(MethodInfo method) {
		if (method.isStatic()) throw new IllegalArgumentException("Static method: " + method);
		return (parser, value, to, implicit) -> InsnTrees.invokeInstance(value, method);
	}

	public static CastHandler invokeStatic(MethodInfo method) {
		if (!method.isStatic()) throw new IllegalArgumentException("Non-static method: " + method);
		return (parser, value, to, implicit) -> InsnTrees.invokeStatic(method, value);
	}

	public static CastHandler invoke(Class<?> in, String name) {
		MethodInfo method = MethodInfo.getMethod(in, name);
		return method.isStatic() ? invokeStatic(method) : invokeVirtual(method);
	}

	public static CastHandlerData data(TypeInfo from, TypeInfo to, boolean implicit, CastHandler caster) {
		return new CastHandlerData(from, to, implicit, caster);
	}

	public static CastHandler allOf(CastHandlerData... casters) {
		return new MultiCastHandler(casters);
	}

	public static boolean F2Z(float value) {
		return value == value;
	}

	public static boolean D2Z(double value) {
		return value == value;
	}

	public static int floorInt(float value) {
		int i = (int)(value);
		return i != Integer.MIN_VALUE && ((float)(i)) > value ? i - 1 : i;
	}

	public static int floorInt(double value) {
		int i = (int)(value);
		return i != Integer.MIN_VALUE && ((double)(i)) > value ? i - 1 : i;
	}

	public static long floorLong(float value) {
		long i = (long)(value);
		return i != Long.MIN_VALUE && ((float)(i)) > value ? i - 1 : i;
	}

	public static long floorLong(double value) {
		long i = (long)(value);
		return i != Long.MIN_VALUE && ((double)(i)) > value ? i - 1 : i;
	}

	public static int ceilInt(float value) {
		int i = (int)(value);
		return i != Integer.MAX_VALUE && ((float)(i)) < value ? i + 1 : i;
	}

	public static int ceilInt(double value) {
		int i = (int)(value);
		return i != Integer.MAX_VALUE && ((double)(i)) < value ? i + 1 : i;
	}

	public static long ceilLong(float value) {
		long i = (long)(value);
		return i != Long.MAX_VALUE && ((float)(i)) < value ? i + 1 : i;
	}

	public static long ceilLong(double value) {
		long i = (long)(value);
		return i != Long.MAX_VALUE && ((double)(i)) < value ? i + 1 : i;
	}

	public static int roundInt(float value) {
		return floorInt(value + 0.5F);
	}

	public static int roundInt(double value) {
		return floorInt(value + 0.5D);
	}

	public static long roundLong(float value) {
		return floorLong(value + 0.5F);
	}

	public static long roundLong(double value) {
		return floorLong(value + 0.5D);
	}

	public static Byte makeByte(MethodHandles.Lookup caller, String name, Class<?> type, int value) {
		return Byte.valueOf(BigGlobeMath.toByteExact(value));
	}

	public static Short makeShort(MethodHandles.Lookup caller, String name, Class<?> type, int value) {
		return Short.valueOf(BigGlobeMath.toShortExact(value));
	}

	public static Integer makeInt(MethodHandles.Lookup caller, String name, Class<?> type, int value) {
		return Integer.valueOf(value);
	}

	public static Long makeLong(MethodHandles.Lookup caller, String name, Class<?> type, long value) {
		return Long.valueOf(value);
	}

	public static Float makeFloat(MethodHandles.Lookup caller, String name, Class<?> type, float value) {
		return Float.valueOf(value);
	}

	public static Double makeDouble(MethodHandles.Lookup caller, String name, Class<?> type, double value) {
		return Double.valueOf(value);
	}

	public static Character makeChar(MethodHandles.Lookup caller, String name, Class<?> type, int value) {
		return Character.valueOf(BigGlobeMath.toCharExact(value));
	}

	public static Boolean makeBoolean(MethodHandles.Lookup caller, String name, Class<?> type, int value) {
		return switch (value) {
			case 0 -> Boolean.FALSE;
			case 1 -> Boolean.TRUE;
			default -> throw new IllegalArgumentException("Not a boolean: " + value);
		};
	}
}