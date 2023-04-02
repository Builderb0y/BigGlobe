package builderb0y.scripting.environments;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.ReduceInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MathScriptEnvironment extends MutableScriptEnvironment {

	public static final MathScriptEnvironment INSTANCE = new MathScriptEnvironment();

	public MathScriptEnvironment() {
		this
		.addVariableConstant("pi", Math.PI)
		.addVariableConstant("tau", Math.PI * 2.0D)
		.addVariableConstant("e", Math.E)
		.addVariableConstant("nan", Float.NaN)
		.addVariableConstant("inf", Float.POSITIVE_INFINITY)
		.addFunctionInvokeStatics(Math.class, "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "toRadians", "toDegrees", "exp", "log", "sqrt", "cbrt", "floor", "ceil", "pow", "atan2")
		.addFunctionInvokeStatics(MathScriptEnvironment.class, "exp2", "log2", "asinh", "acosh", "atanh")
		.addFunctionMultiInvokeStatics(Math.class, "abs", "copySign")
		.addFunctionRenamedInvokeStatic("sign", Integer.class, "signum")
		.addFunctionRenamedInvokeStatic("sign", Integer.class, "signum")
		.addFunctionRenamedMultiInvokeStatic("sign", Math.class, "signum")
		.addFunction("mod", (parser, name, arguments) -> {
			ScriptEnvironment.checkArgumentCount(parser, name, 2, arguments);
			return new CastResult(mod(parser, arguments[0], arguments[1]), false);
		})
		.addFunction("isNaN", createNaN(true))
		.addFunction("isNotNaN", createNaN(false))
		.addFunction("min", createReducer())
		.addFunction("max", createReducer())
		.addFunctionMultiInvokeStatics(Interpolator.class, "mixLinear", "mixClamp", "mixSmooth", "mixSmoother", "unmixLinear", "unmixClamp", "unmixSmooth", "unmixSmoother", "clamp")
		.addFunctionRenamedMultiInvokeStatic("smooth", Interpolator.class, "smoothClamp")
		.addFunctionRenamedMultiInvokeStatic("smoother", Interpolator.class, "smootherClamp")
		.addFunctionInvokeStatics(Float.class, "intBitsToFloat", "floatToIntBits")
		.addFunctionInvokeStatics(Double.class, "longBitsToDouble", "doubleToLongBits")
		;
	}

	@Override
	public MutableScriptEnvironment addFunctionInvokeStatic(String name, MethodInfo method) {
		return super.addFunctionInvokeStatic(name, method.pure());
	}

	public static FunctionHandler createNaN(boolean nan) {
		return (parser, name, arguments) -> {
			ScriptEnvironment.checkArgumentCount(parser, name, 1, arguments);
			if (arguments[0].getTypeInfo().isFloat()) {
				return new CastResult(bool(new NaNConditionTree(arguments[0], nan)), false);
			}
			else {
				return null;
			}
		};
	}

	public static FunctionHandler createReducer() {
		return (parser, name, arguments) -> {
			if (arguments.length < 2) throw new ScriptParsingException(name + "() requires at least 2 arguments", parser.input);
			TypeInfo type = TypeInfos.widenUntilSameInt(Arrays.stream(arguments).map(InsnTree::getTypeInfo));
			return new CastResult(
				new ReduceInsnTree(
					method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, type(type.isFloat() ? MathScriptEnvironment.class : Math.class), name, type, type, type),
					Arrays.stream(arguments).map(argument -> {
						return argument.cast(parser, type, CastMode.IMPLICIT_THROW);
					})
					.toArray(InsnTree[]::new)
				),
				false
			);
		};
	}

	public static final double LN2 = Math.log(2.0D);

	public static double exp2(double d) {
		return Math.exp(d * LN2);
	}

	public static double log2(double d) {
		return Math.log(d) / LN2;
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

	public static float max(float a, float b) {
		if (a > b) return a;
		if (b > a) return b;
		if (Float.isNaN(a)) return b;
		if (Float.isNaN(b)) return a;
		return Float.floatToRawIntBits(a) > Float.floatToRawIntBits(b) ? a : b;
	}

	public static double max(double a, double b) {
		if (a > b) return a;
		if (b > a) return b;
		if (Double.isNaN(a)) return b;
		if (Double.isNaN(b)) return a;
		return Double.doubleToRawLongBits(a) > Double.doubleToRawLongBits(b) ? a : b;
	}

	public static float min(float a, float b) {
		if (a < b) return a;
		if (b < a) return b;
		if (Float.isNaN(a)) return b;
		if (Float.isNaN(b)) return a;
		return Float.floatToRawIntBits(a) < Float.floatToRawIntBits(b) ? a : b;
	}

	public static double min(double a, double b) {
		if (a < b) return a;
		if (b < a) return b;
		if (Double.isNaN(a)) return b;
		if (Double.isNaN(b)) return a;
		return Double.doubleToRawLongBits(a) < Double.doubleToRawLongBits(b) ? a : b;
	}

	public static class NaNConditionTree implements ConditionTree {

		public InsnTree value;
		public boolean nan;

		public NaNConditionTree(InsnTree value, boolean nan) {
			this.value = value;
			this.nan = nan;
		}

		@Override
		public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
			ConditionTree.checkLabels(ifTrue, ifFalse);
			this.value.emitBytecode(method);
			boolean doubleWidth = this.value.getTypeInfo().isDoubleWidth();
			method.node.visitInsn(doubleWidth ? DUP2 : DUP);
			method.node.visitInsn(doubleWidth ? DCMPL : FCMPL);
			if (ifTrue != null) {
				method.node.visitJumpInsn(this.nan ? IFNE : IFEQ, ifTrue);
				if (ifFalse != null) {
					method.node.visitJumpInsn(GOTO, ifFalse);
				}
			}
			else {
				method.node.visitJumpInsn(this.nan ? IFEQ : IFNE, ifFalse);
			}
		}
	}
}