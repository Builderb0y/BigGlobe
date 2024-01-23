package builderb0y.bigglobe.columns.scripted;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.DoubleCompareConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.FloatCompareConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.MultiplyInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@UseVerifier(name = "verify", in = DecisionTreeSettings.class, usage = MemberUsage.METHOD_IS_HANDLER)
public class DecisionTreeSettings {

	public final @VerifyNullable DecisionTreeResult result;
	public final @VerifyNullable DecisionTreeCondition condition;
	public final @VerifyNullable RegistryEntry<DecisionTreeSettings> if_true, if_false;

	public DecisionTreeSettings(
		@VerifyNullable DecisionTreeResult result,
		@VerifyNullable DecisionTreeCondition condition,
		@VerifyNullable RegistryEntry<DecisionTreeSettings> if_true,
		@VerifyNullable RegistryEntry<DecisionTreeSettings> if_false
	) {
		this.result    = result;
		this.condition = condition;
		this.if_true   = if_true;
		this.if_false  = if_false;
	}

	@SuppressWarnings("deprecation")
	public static <T_Encoded> void verify(VerifyContext<T_Encoded, DecisionTreeSettings> context) throws VerifyException {
		DecisionTreeSettings object = context.object;
		if (object == null) return;
		if (object.result != null) {
			if (object.condition != null || object.if_true != null || object.if_false != null) {
				throw new VerifyException("Must specify EITHER condition, if_true, and if_false, OR result. But not both.");
			}
		}
		else {
			if (object.condition == null || object.if_true == null || object.if_false == null) {
				throw new VerifyException("Must specify EITHER condition, if_true, and if_false, OR result. But not both.");
			}
		}
	}

	public InsnTree createInsnTree(
		RegistryEntry<DecisionTreeSettings> selfEntry,
		long seed,
		DataCompileContext context,
		@Nullable InsnTree loadY
	) {
		try {
			if (this.result != null) {
				return this.result.createResult(seed, context, loadY);
			}
			else {
				ConditionTree condition = this.condition.createCondition(seed, context, loadY);
				InsnTree ifTrue = this.if_true.value().createInsnTree(this.if_true, Permuter.permute(seed, 1), context, loadY);
				InsnTree ifFalse = this.if_false.value().createInsnTree(this.if_false, Permuter.permute(seed, -1), context, loadY);
				if (!ifTrue.getTypeInfo().equals(ifFalse.getTypeInfo())) {
					throw new DecisionTreeException(UnregisteredObjectException.getKey(this.if_true) + " and " + UnregisteredObjectException.getKey(this.if_false) + " do not have the same return type.");
				}
				return new IfElseInsnTree(condition, ifTrue, ifFalse, ifTrue.getTypeInfo());
			}
		}
		catch (Exception exception) {
			DecisionTreeException detailedException = exception instanceof DecisionTreeException e ? e : new DecisionTreeException(exception);
			detailedException.details.add("Used by " + UnregisteredObjectException.getKey(selfEntry));
			throw detailedException;
		}
	}

	@UseCoder(name = "REGISTRY", in = DecisionTreeCondition.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
	public static interface DecisionTreeCondition extends CoderRegistryTyped<DecisionTreeCondition> {

		public static final CoderRegistry<DecisionTreeCondition> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("decision_tree_condition"));
		public static final Object INITIALIZER = new Object() {{
			REGISTRY.registerAuto(BigGlobeMod.modID("range"), RangeDecisionTreeCondition.class);
		}};

		public abstract ConditionTree createCondition(long seed, DataCompileContext context, @Nullable InsnTree loadY);
	}

	public static class RangeDecisionTreeCondition implements DecisionTreeCondition {

		public final Identifier column_value;
		public final double min, max;
		public final @DefaultBoolean(true) boolean smooth_min, smooth_max;

		public RangeDecisionTreeCondition(
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
		public ConditionTree createCondition(long seed, DataCompileContext context, @Nullable InsnTree loadY) {
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
					invokeStatic(
						MethodInfo.findMethod(Permuter.class, "toPositiveFloat", float.class, long.class),
						invokeInstance(
							context.loadColumn(),
							MethodInfo.findMethod(ScriptedColumn.class, "columnSeed", long.class, long.class),
							ldc(seed)
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
					invokeStatic(
						MethodInfo.findMethod(Permuter.class, "toPositiveDouble", double.class, long.class),
						invokeInstance(
							context.loadColumn(),
							MethodInfo.findMethod(ScriptedColumn.class, "columnSeed", long.class, long.class),
							ldc(seed)
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

	@UseCoder(name = "REGISTRY", in = DecisionTreeResult.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
	public static interface DecisionTreeResult extends CoderRegistryTyped<DecisionTreeResult> {

		public static final CoderRegistry<DecisionTreeResult> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("decision_tree_result"));
		public static final Object INITIALIZER = new Object() {{
			REGISTRY.registerAuto(BigGlobeMod.modID("block_state_constant"), BlockStateConstantDecisionTreeResult.class);
		}};

		public abstract InsnTree createResult(long seed, DataCompileContext context, @Nullable InsnTree loadY);
	}

	public static class BlockStateConstantDecisionTreeResult implements DecisionTreeResult {

		public final BlockState state;

		public BlockStateConstantDecisionTreeResult(BlockState state) {
			this.state = state;
		}

		@Override
		public InsnTree createResult(long seed, DataCompileContext context, @Nullable InsnTree loadY) {
			return ldc(this.state, type(BlockState.class));
		}
	}

	public static class DecisionTreeException extends RuntimeException {

		public List<String> details = new ArrayList<>(8);

		public DecisionTreeException() {}

		public DecisionTreeException(String message) {
			super(message);
		}

		public DecisionTreeException(String message, Throwable cause) {
			super(message, cause);
		}

		public DecisionTreeException(Throwable cause) {
			super(cause);
		}
	}
}