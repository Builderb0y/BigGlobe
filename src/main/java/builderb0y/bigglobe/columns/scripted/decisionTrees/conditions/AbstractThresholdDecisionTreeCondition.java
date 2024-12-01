package builderb0y.bigglobe.columns.scripted.decisionTrees.conditions;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeException;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.DoubleCompareConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.FloatCompareConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.MultiplyInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class AbstractThresholdDecisionTreeCondition extends DecisionTreeCondition.Impl {

	public final double min, max;
	public final @DefaultBoolean(true) boolean smooth_min, smooth_max;

	public AbstractThresholdDecisionTreeCondition(
		double min,
		double max,
		@DefaultBoolean(true) boolean smooth_min,
		@DefaultBoolean(true) boolean smooth_max
	) {
		this.min = min;
		this.max = max;
		this.smooth_min = smooth_min;
		this.smooth_max = smooth_max;
	}

	public ConditionTree finishCondition(InsnTree value, long selfSeed, DataCompileContext context) {
		return switch (value.getTypeInfo().getSort()) {
			case FLOAT -> FloatCompareConditionTree.lessThan(
				RandomScriptEnvironment.PERMUTER_INFO.toPositiveFloat(
					ScriptedColumn.INFO.saltedPositionedSeed(
						context.loadColumn(),
						ldc(selfSeed)
					)
				),
				invokeStatic(
					MethodInfo.inCaller(
						this.smooth_min
						? (this.smooth_max ? "smoothBothF" : "smoothMinF")
						: (this.smooth_max ? "smoothMaxF" : "smoothNoneF")
					),
					new MultiplyInsnTree(
						new SubtractInsnTree(
							value,
							ldc((float)(this.min)),
							FSUB
						),
						ldc((float)(1.0D / (this.max - this.min))),
						FMUL
					)
				)
			);
			case DOUBLE -> DoubleCompareConditionTree.lessThan(
				RandomScriptEnvironment.PERMUTER_INFO.toPositiveDouble(
					ScriptedColumn.INFO.saltedPositionedSeed(
						context.loadColumn(),
						ldc(selfSeed)
					)
				),
				invokeStatic(
					MethodInfo.inCaller(
						this.smooth_min
						? (this.smooth_max ? "smoothBothD" : "smoothMinD")
						: (this.smooth_max ? "smoothMaxD" : "smoothNoneD")
					),
					new MultiplyInsnTree(
						new SubtractInsnTree(
							value,
							ldc(this.min),
							DSUB
						),
						ldc(1.0D / (this.max - this.min)),
						DMUL
					)
				)
			);
			default -> throw new DecisionTreeException(this.errorMessage(value.getTypeInfo()));
		};
	}

	public abstract String errorMessage(TypeInfo type);

	public static float smoothNoneF(float value) {
		if (!(value > 0.0F)) return 0.0F;
		if (!(value < 1.0F)) return 1.0F;
		return value;
	}

	public static float smoothMinF(float value) {
		if (!(value > 0.0F)) return 0.0F;
		if (!(value < 1.0F)) return 1.0F;
		return value * value;
	}

	public static float smoothMaxF(float value) {
		if (!(value > 0.0F)) return 0.0F;
		if (!(value < 1.0F)) return 1.0F;
		return value * (2.0F - value);
	}

	public static float smoothBothF(float value) {
		if (!(value > 0.0F)) return 0.0F;
		if (!(value < 1.0F)) return 1.0F;
		return value * value * (value * -2.0F + 3.0F);
	}

	public static double smoothNoneD(double value) {
		if (!(value > 0.0D)) return 0.0D;
		if (!(value < 1.0D)) return 1.0D;
		return value;
	}

	public static double smoothMinD(double value) {
		if (!(value > 0.0D)) return 0.0D;
		if (!(value < 1.0D)) return 1.0D;
		return value * value;
	}

	public static double smoothMaxD(double value) {
		if (!(value > 0.0D)) return 0.0D;
		if (!(value < 1.0D)) return 1.0D;
		return value * (2.0D - value);
	}

	public static double smoothBothD(double value) {
		if (!(value > 0.0D)) return 0.0D;
		if (!(value < 1.0D)) return 1.0D;
		return value * value * (value * -2.0D + 3.0D);
	}
}