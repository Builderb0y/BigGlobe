package builderb0y.scripting.environments;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.ReduceInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MathScriptEnvironment extends MutableScriptEnvironment2 {

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
		.addFunctionMultiInvokeStatic(Math.class, "abs")
		.addFunctionRenamedInvokeStatic("sign", Integer.class, "signum")
		.addFunctionRenamedInvokeStatic("sign", Integer.class, "signum")
		.addFunctionRenamedMultiInvokeStatic("sign", Math.class, "signum")
		.addFunction("mod", (parser, name, arguments) -> {
			ScriptEnvironment.checkArgumentCount(parser, name, 2, arguments);
			return mod(parser, arguments[0], arguments[1]);
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

	public static FunctionHandler createNaN(boolean nan) {
		return (parser, name, arguments) -> {
			ScriptEnvironment.checkArgumentCount(parser, name, 1, arguments);
			if (arguments[0].getTypeInfo().isFloat()) {
				return bool(new NaNConditionTree(arguments[0], nan));
			}
			else {
				throw new ScriptParsingException(name + "() requires a float or double as an argument", parser.input);
			}
		};
	}

	public static FunctionHandler createReducer() {
		return (parser, name, arguments) -> {
			if (arguments.length < 2) throw new ScriptParsingException(name + "() requires at least 2 arguments", parser.input);
			TypeInfo type = TypeInfos.widenUntilSameInt(Arrays.stream(arguments).map(InsnTree::getTypeInfo));
			return new ReduceInsnTree(
				method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, type(Math.class), name, type, type, type),
				Arrays.stream(arguments).map(argument -> {
					return argument.cast(parser, type, CastMode.IMPLICIT_THROW);
				})
				.toArray(InsnTree[]::new)
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