package builderb0y.bigglobe.columns.scripted.decisionTrees;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.DoubleCompareConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.FloatCompareConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.MultiplyInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ThresholdDecisionTreeCondition implements DecisionTreeCondition {

	public final Identifier column_value;
	public final double min, max;
	public final @DefaultBoolean(true) boolean smooth_min, smooth_max;

	public ThresholdDecisionTreeCondition(
		Identifier column_value,
		double min,
		double max,
		@DefaultBoolean(true) boolean smooth_min,
		@DefaultBoolean(true) boolean smooth_max
	) {
		this.column_value = column_value;
		this.min = min;
		this.max = max;
		this.smooth_min = smooth_min;
		this.smooth_max = smooth_max;
	}

	@Override
	public ConditionTree createCondition(RegistryEntry<DecisionTreeSettings> selfEntry, long selfSeed, DataCompileContext context, @Nullable InsnTree loadY) throws ScriptParsingException {
		ColumnEntryMemory memory = context.root().registry.memories.get(this.column_value);
		if (memory == null) {
			throw new DecisionTreeException("Unknown column value: " + this.column_value);
		}
		MethodCompileContext getter = memory.getTyped(ColumnEntryMemory.GETTER);
		boolean requiresY = getter.info.paramTypes.length != 0;
		if (requiresY && loadY == null) {
			throw new DecisionTreeException(this.column_value + " is 3D, but a Y level is not provided.");
		}
		return switch (getter.info.returnType.getSort()) {
			case FLOAT -> FloatCompareConditionTree.lessThan(
				RandomScriptEnvironment.PERMUTER_INFO.toPositiveFloat(
					ScriptedColumn.INFO.saltedSeed(
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
							invokeInstance(
								context.loadSelf(),
								getter.info,
								requiresY ? new InsnTree[] { loadY } : InsnTree.ARRAY_FACTORY.empty()
							),
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
					ScriptedColumn.INFO.saltedSeed(
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
							invokeInstance(
								context.loadSelf(),
								getter.info,
								requiresY ? new InsnTree[] { loadY } : InsnTree.ARRAY_FACTORY.empty()
							),
							ldc(this.min),
							DSUB
						),
						ldc(1.0D / (this.max - this.min)),
						DMUL
					)
				)
			);
			default -> throw new DecisionTreeException("range decision tree only works with float and double typed column values, but " + this.column_value + " is a " + getter.info.returnType);
		};
	}

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