package builderb0y.scripting.bytecode;

import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.I2ZInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastHandlerData;
import builderb0y.scripting.environments.MutableScriptEnvironment.MultiCastHandler;
import builderb0y.scripting.util.TypeInfos;

public class CastingSupport {

	public static final CastHandlerData
		I2B = data(TypeInfos.INT, TypeInfos.BYTE, false, opcode(Opcodes.I2B)),
		I2S = data(TypeInfos.INT, TypeInfos.SHORT, false, opcode(Opcodes.I2S)),
		I2C = data(TypeInfos.INT, TypeInfos.CHAR, false, opcode(Opcodes.I2C)),
		I2L = data(TypeInfos.INT, TypeInfos.LONG, true, opcode(Opcodes.I2L)),
		I2F = data(TypeInfos.INT, TypeInfos.FLOAT, true, opcode(Opcodes.I2F)),
		I2D = data(TypeInfos.INT, TypeInfos.DOUBLE, true, opcode(Opcodes.I2D)),
		L2I = data(TypeInfos.LONG, TypeInfos.INT, false, opcode(Opcodes.L2I)),
		L2F = data(TypeInfos.LONG, TypeInfos.FLOAT, true, opcode(Opcodes.L2F)),
		L2D = data(TypeInfos.LONG, TypeInfos.DOUBLE, true, opcode(Opcodes.L2D)),
		F2I = data(TypeInfos.FLOAT, TypeInfos.INT, false, invokeStatic(MethodInfo.findMethod(CastingSupport.class, "floorInt", int.class, float.class))),
		F2L = data(TypeInfos.FLOAT, TypeInfos.LONG, false, invokeStatic(MethodInfo.findMethod(CastingSupport.class, "floorLong", long.class, float.class))),
		F2D = data(TypeInfos.FLOAT, TypeInfos.DOUBLE, true, opcode(Opcodes.F2D)),
		D2I = data(TypeInfos.DOUBLE, TypeInfos.INT, false, invokeStatic(MethodInfo.findMethod(CastingSupport.class, "floorInt", int.class, double.class))),
		D2L = data(TypeInfos.DOUBLE, TypeInfos.LONG, false, invokeStatic(MethodInfo.findMethod(CastingSupport.class, "floorLong", long.class, double.class))),
		D2F = data(TypeInfos.DOUBLE, TypeInfos.FLOAT, false, opcode(Opcodes.D2F)),
		I2Z = data(TypeInfos.INT, TypeInfos.BOOLEAN, false, (parser, value, to) -> new I2ZInsnTree(value)),
		L2Z = data(
			TypeInfos.LONG,
			TypeInfos.BOOLEAN,
			false,
			allOf(
				data(TypeInfos.LONG, TypeInfos.INT, false, opcode(Opcodes.LCMP)),
				I2Z
			)
		),
		F2Z = data(TypeInfos.FLOAT, TypeInfos.BOOLEAN, false, invoke(CastingSupport.class, "F2Z")),
		D2Z = data(TypeInfos.DOUBLE, TypeInfos.BOOLEAN, false, invoke(CastingSupport.class, "D2Z"));

	public static InsnTree primitiveCast(InsnTree value, TypeInfo type) {
		//passing in a null parser is NOT recommended, but in this case it is safe.
		return BuiltinScriptEnvironment.INSTANCE.cast(null, value, type, false);
	}

	public static CastHandler opcode(int opcode) {
		return (parser, value, to) -> new OpcodeCastInsnTree(value, opcode, to);
	}

	public static CastHandler invokeVirtual(MethodInfo method) {
		if (method.isStatic()) throw new IllegalArgumentException("Static method: " + method);
		return (parser, value, to) -> InsnTrees.invokeVirtualOrInterface(value, method);
	}

	public static CastHandler invokeStatic(MethodInfo method) {
		if (!method.isStatic()) throw new IllegalArgumentException("Non-static method: " + method);
		return (parser, value, to) -> InsnTrees.invokeStatic(method, value);
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
		return value != 0.0F && value == value;
	}

	public static boolean D2Z(double value) {
		return value != 0.0D && value == value;
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
}