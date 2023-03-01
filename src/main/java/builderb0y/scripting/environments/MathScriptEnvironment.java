package builderb0y.scripting.environments;

import java.util.Arrays;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.ReduceInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MathScriptEnvironment extends MutableScriptEnvironment {

	public static final MathScriptEnvironment INSTANCE = new MathScriptEnvironment();

	public MathScriptEnvironment() {
		this
		.addVariable("pi",  ldc(Math.PI))
		.addVariable("tau", ldc(Math.PI * 2.0D))
		.addVariable("e",   ldc(Math.E))
		.addVariable("nan", ldc(Float.NaN))
		.addVariable("inf", ldc(Float.POSITIVE_INFINITY));
		for (String name : new String[] {
			"sin", "cos", "tan",
			"asin", "acos", "atan",
			"sinh", "cosh", "tanh",
			"toRadians", "toDegrees",
			"exp", "log",
			"sqrt", "cbrt",
			"floor", "ceil"
		}) {
			this.addFloatCompatibleFunction(name, method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Math.class, name, double.class, double.class));
		}
		for (String name : new String[] { "pow", "atan2" }) {
			this.addFloatCompatibleFunction(name, method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Math.class, name, double.class, double.class, double.class));
		}
		for (String name : new String[] { "exp2", "log2" }) {
			this.addFloatCompatibleFunction(name, method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, BigGlobeMath.class, name, double.class, double.class));
		}
		for (String name : new String[] { "asinh", "acosh", "atanh" }) {
			this.addFloatCompatibleFunction(name, method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, MathScriptEnvironment.class, name, double.class, double.class));
		}
		this
		.addFunction("abs", FunctionHandler.ofAll(
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Math.class, "abs", int.class, int.class),
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Math.class, "abs", long.class, long.class),
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Math.class, "abs", float.class, float.class),
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Math.class, "abs", double.class, double.class)
		))
		.addFunction("sign", FunctionHandler.ofAll(
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Integer.class, "signum", int.class, int.class),
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Long.class, "signum", long.class, long.class),
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Math.class, "signum", float.class, float.class),
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Math.class, "signum", double.class, double.class)
		))
		.addFunction("mod", (parser, name, arguments) -> {
			ScriptEnvironment.checkArgumentCount(parser, name, 2, arguments);
			return mod(parser, arguments[0], arguments[1]);
		})
		.addFunction("isNaN", FunctionHandler.ofAll(
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Float.class, "isNaN", boolean.class, float.class),
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Double.class, "isNaN", boolean.class, double.class)
		))
		.addFunction("isNotNaN", FunctionHandler.ofAll(
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, MathScriptEnvironment.class, "isNaN", boolean.class, float.class),
			method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, MathScriptEnvironment.class, "isNaN", boolean.class, double.class)
		));
		FunctionHandler minMax = (parser, name, arguments) -> {
			if (arguments.length < 2) {
				throw new ScriptParsingException(name + "requires at least 2 arguments, got " + arguments.length, parser.input);
			}
			TypeInfo type = TypeInfos.widenUntilSameInt(Arrays.stream(arguments).map(InsnTree::getTypeInfo));
			return new ReduceInsnTree(
				method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, type(Math.class), name, type, type, type),
				Arrays.stream(arguments).map(argument -> {
					return argument.cast(parser, type, CastMode.EXPLICIT_THROW);
				})
				.toArray(InsnTree[]::new)
			);
		};
		this.addFunction("min", minMax).addFunction("max", minMax);

		for (String name : new String[] {
			"mixLinear", "mixClamp", "mixSmooth", "mixSmoother",
			"unmixLinear", "unmixClamp", "unmixSmooth", "unmixSmoother",
			"clamp"
		}) {
			this.addInterpolatorFunction(name, 3);
		}
		this.addInterpolatorFunction("smooth", 1);
		this.addInterpolatorFunction("smoother", 1);
		this.addInterpolatorFunction("smoothClamp", 3);
		this.addInterpolatorFunction("clampSmooth", "smoothClamp", 3);
		this.addInterpolatorFunction("smootherClamp", 3);
		this.addInterpolatorFunction("clampSmoother", "smootherClamp", 3);

		this
		.addFunction("intToFloatBits", FunctionHandler.of(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Float.class, "intToFloatBits", float.class, int.class)))
		.addFunction("floatToIntBits", FunctionHandler.of(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Float.class, "floatToIntBits", int.class, float.class)))
		.addFunction("longToDoubleBits", FunctionHandler.of(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Integer.class, "longToDoubleBits", double.class, long.class)))
		.addFunction("doubleToLongBits", FunctionHandler.of(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Integer.class, "doubleToLongBits", long.class, double.class)));
	}

	public void addFloatCompatibleFunction(String name, MethodInfo info) {
		this.addFunction(name, (parser, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name1, info.paramTypes, CastMode.IMPLICIT_THROW, arguments);
			InsnTree result = invokeStatic(info, castArguments);
			if (Arrays.stream(arguments).map(InsnTree::getTypeInfo).map(TypeInfo::getSort).allMatch(Sort.FLOAT::equals)) {
				result = result.cast(parser, TypeInfos.FLOAT, CastMode.EXPLICIT_THROW);
			}
			return result;
		});
	}

	public void addInterpolatorFunction(String name, int argCount) {
		this.addInterpolatorFunction(name, name, argCount);
	}

	public void addInterpolatorFunction(String name, String internalName, int argCount) {
		MethodInfo  floatMethod = method(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Interpolator.class), internalName, TypeInfos.FLOAT,  types("F".repeat(argCount)));
		MethodInfo doubleMethod = method(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Interpolator.class), internalName, TypeInfos.DOUBLE, types("D".repeat(argCount)));
		this.addFunction(name, (parser, name1, arguments) -> {
			MethodInfo method = (
				Arrays
				.stream(arguments)
				.allMatch(argument -> argument.getTypeInfo().isSingleWidth())
			)
			? floatMethod
			: doubleMethod;
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name1, method.paramTypes, CastMode.IMPLICIT_THROW, arguments);
			return invokeStatic(method, castArguments);
		});
	}

	public static double asinh(double x) {
		return Math.log(Math.sqrt(x * x + 1.0D) + x);
	}

	public static double acosh(double x) {
		return Math.log(Math.sqrt(x * x - 1.0D) + x);
	}

	public static double atanh(double x) {
		//alternate form: log(2 / (1 - x) - 1) * 0.5
		return Math.log((1.0D + x) / (1.0D - x)) * 0.5D;
	}

	public static boolean isNotNaN(float value) {
		return value == value;
	}

	public static boolean isNotNaN(double value) {
		return value == value;
	}
}