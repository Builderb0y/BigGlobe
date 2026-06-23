package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.data.AbstractNumberData;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.NumberData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.WorldTrait;
import builderb0y.bigglobe.columns.scripted.tree.StandAloneTraits2DGetterInsnTree;
import builderb0y.bigglobe.columns.scripted.tree.StandAloneTraits3DGetterInsnTree;
import builderb0y.bigglobe.math.GeneralSmoothstep;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@UseCoder(name = "REGISTRY", in = ConditionProvider.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface ConditionProvider extends SimpleDependencyView, CoderRegistryTyped<ConditionProvider> {

	public static final CoderRegistry<ConditionProvider> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("decision_tree_condition_provider")) {

		@Override
		@OverrideOnly
		public <T_Encoded> @VerifyNullable ConditionProvider decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			AbstractNumberData number = context.data().tryAsNumber();
			if (number != null) {
				return new ConstantConditionProvider(number.doubleValue());
			}
			else {
				return super.decode(context);
			}
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, ConditionProvider> context) throws EncodeException {
			if (context.object instanceof ConstantConditionProvider provider) {
				return new NumberData(provider.chance);
			}
			else {
				return super.encode(context);
			}
		}
	};
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("constant"),        ConstantConditionProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("column_value"), ColumnValueConditionProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("world_trait"),   WorldTraitConditionProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("script"),          ScriptedConditionProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("curve_range"),        CurveConditionProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("and"),                  AndConditionProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("or"),                    OrConditionProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("not"),                  NotConditionProvider.class);
	}};

	public static final MethodInfo
		clampF = MethodInfo.inCaller("clampF"),
		clampD = MethodInfo.inCaller("clampD");

	public static float clampF(float f) {
		return f > 0.0F ? f > 1.0F ? 1.0F : f : 0.0F;
	}

	public static double clampD(double d) {
		return d > 0.0D ? d > 1.0D ? 1.0D : d : 0.0D;
	}

	public static InsnTree clamp(InsnTree tree) {
		return invokeStatic(
			switch (tree.getTypeInfo().getSort()) {
				case FLOAT -> clampF;
				case DOUBLE -> clampD;
				default -> throw new IllegalArgumentException();
			},
			tree
		);
	}

	public abstract InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException;

	public abstract ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException;

	public static ConditionTree chanceToBoolean(DecisionTreeContext context, InsnTree maybeChance, long seedSalt, boolean is3D) {
		return condition(
			CastingSupport.dummyParser(),
			switch (maybeChance.getTypeInfo().getSort()) {
				case FLOAT -> RandomScriptEnvironment.PERMUTER_INFO.nextChancedBooleanF(
					is3D
					? ScriptedColumn.INFO.saltedPositionedSeed3D(context.loadColumn(), ldc(seedSalt), load("y", TypeInfos.INT))
					: ScriptedColumn.INFO.saltedPositionedSeed(context.loadColumn(), ldc(seedSalt)),
					maybeChance
				);
				case DOUBLE -> RandomScriptEnvironment.PERMUTER_INFO.nextChancedBooleanD(
					is3D
					? ScriptedColumn.INFO.saltedPositionedSeed3D(context.loadColumn(), ldc(seedSalt), load("y", TypeInfos.INT))
					: ScriptedColumn.INFO.saltedPositionedSeed(context.loadColumn(), ldc(seedSalt)),
					maybeChance
				);
				case BOOLEAN -> maybeChance;
				default -> throw new IllegalArgumentException(maybeChance.toString());
			}
		);
	}

	public default ConditionTree chanceToBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean is3D) throws ScriptParsingException, ConstantFormatException {
		return chanceToBoolean(context, this.emitChance(context, caller, seedSalt, false), seedSalt, is3D);
	}

	public record ConstantConditionProvider(@VerifyFloatRange(min = 0.0D, max = 1.0D) double chance) implements ConditionProvider {

		@Override
		public InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException {
			double d = this.chance;
			float f = (float)(d);
			return f == d ? ldc(f) : ldc(d);
		}

		@Override
		public ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			if (!(this.chance > 0.0D)) return ConstantConditionTree.FALSE;
			if (!(this.chance < 1.0D)) return ConstantConditionTree.TRUE;
			return this.chanceToBoolean(context, caller, seedSalt, false);
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.empty();
		}
	}

	public record ColumnValueConditionProvider(Holder<ColumnEntry> column_value) implements ConditionProvider {

		@Override
		public InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException {
			ColumnEntry columnEntry = this.column_value.value();
			TypeInfo existingType = columnEntry.getTypeInfo(context.columnEntryRegistry);
			if (clamp ? !existingType.isFloat() : !existingType.isNumber()) {
				throw new ScriptParsingException("Column value " + UnregisteredObjectException.getID(this.column_value) + " is not a " + (clamp ? "floating point value." : "number."), null);
			}
			boolean is3D = columnEntry.params.is_3d();
			if (is3D && !context.is3D) {
				throw new ScriptParsingException("Column value " + UnregisteredObjectException.getID(this.column_value) + " is 3D, but an implicit Y level is not available here.", null);
			}
			MethodCompileContext method = context.columnEntryRegistry.columnCompileContext.getCompileContext(columnEntry).mainGetter;
			InsnTree result = invokeInstance(context.loadColumn(), method.info, is3D ? new InsnTree[] { load("y", TypeInfos.INT) } : InsnTree.ARRAY_FACTORY.empty());
			if (!existingType.isFloat()) {
				result = result.cast(CastingSupport.dummyParser(), TypeInfos.DOUBLE, CastMode.IMPLICIT_THROW, false);
			}
			if (clamp) result = clamp(result);
			return result;
		}

		@Override
		public ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			ColumnEntry columnEntry = this.column_value.value();
			TypeInfo existingType = columnEntry.getTypeInfo(context.columnEntryRegistry);
			if (!existingType.isFloat() && !existingType.isBoolean()) {
				throw new ScriptParsingException("Column value " + UnregisteredObjectException.getID(this.column_value) + " is not a floating point value or a boolean.", null);
			}
			boolean is3D = columnEntry.params.is_3d();
			if (is3D && !context.is3D) {
				throw new ScriptParsingException("Column value " + UnregisteredObjectException.getID(this.column_value) + " is 3D, but an implicit Y level is not available here.", null);
			}
			MethodCompileContext method = context.columnEntryRegistry.columnCompileContext.getCompileContext(columnEntry).mainGetter;
			InsnTree result = invokeInstance(context.loadColumn(), method.info, DecisionTreeContext.yArguments(is3D));
			return ConditionProvider.chanceToBoolean(context, result, seedSalt, is3D);
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.of(this.column_value);
		}
	}

	public record WorldTraitConditionProvider(Holder<WorldTrait> world_trait) implements ConditionProvider {

		@Override
		public InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException {
			WorldTrait worldTrait = this.world_trait.value();
			TypeInfo existingType = worldTrait.getTypeInfo(context.columnEntryRegistry.traitManager);
			if (clamp ? !existingType.isFloat() : !existingType.isNumber()) {
				throw new ScriptParsingException("World trait " + UnregisteredObjectException.getID(this.world_trait) + " is not a " + (clamp ? "floating point value." : "number."), null);
			}
			boolean is3D = worldTrait.schema().is_3d();
			if (is3D && !context.is3D) {
				throw new ScriptParsingException("World trait " + UnregisteredObjectException.getID(this.world_trait) + " is 3D, but an implicit Y level is not available here.", null);
			}
			MethodInfo traitGetter = context.columnEntryRegistry.traitManager.infos.get(this.world_trait).getter.info;
			InsnTree result = (
				is3D
				? new StandAloneTraits3DGetterInsnTree(context.loadColumn(), context.yArgument(), traitGetter, null)
				: new StandAloneTraits2DGetterInsnTree(context.loadColumn(), traitGetter, null)
			);
			if (!existingType.isFloat()) {
				result = result.cast(CastingSupport.dummyParser(), TypeInfos.DOUBLE, CastMode.IMPLICIT_THROW, false);
			}
			if (clamp) result = clamp(result);
			return result;
		}

		@Override
		public ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			WorldTrait worldTrait = this.world_trait.value();
			TypeInfo existingType = worldTrait.getTypeInfo(context.columnEntryRegistry.traitManager);
			if (!existingType.isFloat() && !existingType.isBoolean()) {
				throw new ScriptParsingException("World trait " + UnregisteredObjectException.getID(this.world_trait) + " is not a floating point value or a boolean.", null);
			}
			boolean is3D = worldTrait.schema().is_3d();
			if (is3D && !context.is3D) {
				throw new ScriptParsingException("World trait " + UnregisteredObjectException.getID(this.world_trait) + " is 3D, but an implicit Y level is not available here.", null);
			}
			MethodInfo traitGetter = context.columnEntryRegistry.traitManager.infos.get(this.world_trait).getter.info;
			InsnTree result = (
				is3D
				? new StandAloneTraits3DGetterInsnTree(context.loadColumn(), context.yArgument(), traitGetter, null)
				: new StandAloneTraits2DGetterInsnTree(context.loadColumn(), traitGetter, null)
			);
			return ConditionProvider.chanceToBoolean(context, result, seedSalt, is3D);
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.of(this.world_trait);
		}
	}

	public static class ScriptedConditionProvider implements ConditionProvider, SetBasedMutableDependencyView {

		public final ScriptUsage script;
		public final transient Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

		public ScriptedConditionProvider(ScriptUsage script) {
			this.script = script;
		}

		@Override
		public InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException {
			MethodCompileContext method = context.newMethod("decision_tree_value_", caller, TypeInfos.DOUBLE);
			context.columnEntryRegistry.setMethodCode(method, this.script, context.loadColumn(), context.yArgument(), null, null, this, (ExpressionParser parser) -> {
				parser.environment.mutable().addVariable("fuzzSeed", context.loadSeed(ldc(seedSalt)));
			});
			InsnTree result = invokeInstance(context.loadColumn(), method.info, context.yArguments());
			if (clamp) result = clamp(result);
			return result;
		}

		@Override
		public ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			MethodCompileContext method = context.newMethod("decision_tree_value_", caller, TypeInfos.BOOLEAN);
			context.columnEntryRegistry.setMethodCode(method, this.script, context.loadColumn(), context.yArgument(), null, null, this, (ExpressionParser parser) -> {
				parser.environment.mutable().addVariable("fuzzSeed", context.loadSeed(ldc(seedSalt)));
			});
			InsnTree result = invokeInstance(context.loadColumn(), method.info, context.yArguments());
			return ConditionProvider.chanceToBoolean(context, result, seedSalt, context.is3D);
		}

		@Override
		public Set<Holder<? extends DependencyView>> getDependencies() {
			return this.dependencies;
		}
	}

	public static record CurveConditionProvider(
		ConditionProvider value,
		@DefaultFloat(value = 0.0F, mode = DefaultMode.ENCODED) ConditionProvider min,
		@DefaultFloat(value = 1.0F, mode = DefaultMode.ENCODED) ConditionProvider max,
		@DefaultInt(1) @VerifyIntRange(min = 0) int smooth_min,
		@DefaultInt(1) @VerifyIntRange(min = 0) int smooth_max
	)
	implements ConditionProvider {

		public static final MethodInfo
			unmixF = MethodInfo.inCaller("unmixF"),
			unmixD = MethodInfo.inCaller("unmixD");

		@Override
		public InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException {
			InsnTree value = this.value.emitChance(context, caller, seedSalt, false);
			InsnTree unmix;
			if (this.min instanceof ConstantConditionProvider(double minValue) && minValue == 0.0D && this.max instanceof ConstantConditionProvider(double maxValue) && maxValue == 1.0D) {
				unmix = value;
			}
			else {
				InsnTree min = this.min.emitChance(context, caller, seedSalt, false);
				InsnTree max = this.max.emitChance(context, caller, seedSalt, false);
				TypeInfo precision = TypeInfos.widenUntilSame(
					TypeInfos.FLOAT,
					value.getTypeInfo(),
					min  .getTypeInfo(),
					max  .getTypeInfo()
				);
				value = CastingSupport.primitiveCast(value, precision);
				min   = CastingSupport.primitiveCast(min,   precision);
				max   = CastingSupport.primitiveCast(max,   precision);
				MethodInfo unmixer = switch (precision.getSort()) {
					case FLOAT  -> unmixF;
					case DOUBLE -> unmixD;
					default -> throw new AssertionError(precision);
				};
				unmix = invokeStatic(unmixer, value, min, max);
			}
			return invokeDynamic(
				GeneralSmoothstep.BSM,
				new MethodInfo(
					ACC_PUBLIC | ACC_STATIC,
					TypeInfos.OBJECT, //unused.
					"curve",
					unmix.getTypeInfo(),
					unmix.getTypeInfo()
				),
				new ConstantValue[] {
					constant(this.smooth_min),
					constant(this.smooth_max)
				},
				new InsnTree[] {
					unmix
				}
			);
		}

		@Override
		public ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			return this.chanceToBoolean(context, caller, seedSalt, context.is3D);
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.of(this.value, this.min, this.max).flatMap(ConditionProvider::streamDirectDependencies);
		}

		public static float unmixF(float value, float min, float max) {
			return (value - min) / (max - min);
		}

		public static double unmixD(double value, double min, double max) {
			return (value - min) / (max - min);
		}
	}

	public static record AndConditionProvider(@VerifySizeRange(min = 1) List<ConditionProvider> conditions) implements ConditionProvider {

		@Override
		public InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException {
			TypeInfo type = TypeInfos.FLOAT;
			InsnTree[] toMultiply = new InsnTree[this.conditions.size()];
			for (int index = 0; index < toMultiply.length; index++) {
				type = TypeInfos.widenUntilSame(type, (toMultiply[index] = this.conditions.get(index).emitChance(context, caller, Permuter.permute(seedSalt, index), true)).getTypeInfo());
			}
			for (int index = 0; index < toMultiply.length; index++) {
				toMultiply[index] = CastingSupport.primitiveCast(toMultiply[index], type);
			}
			return mul(CastingSupport.dummyParser(), toMultiply);
		}

		@Override
		public ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			ConditionTree result = this.conditions.getFirst().emitBoolean(context, caller, Permuter.permute(seedSalt, 0));
			for (int index = 0; index < this.conditions.size(); index++) {
				result = and(result, this.conditions.get(index).emitBoolean(context, caller, Permuter.permute(seedSalt, index)));
			}
			return result;
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return this.conditions.stream().flatMap(ConditionProvider::streamDirectDependencies);
		}
	}

	public static record OrConditionProvider(@VerifySizeRange(min = 1) List<ConditionProvider> conditions) implements ConditionProvider {

		@Override
		public InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException {
			InsnTree first = this.conditions.getFirst().emitChance(context, caller, Permuter.permute(seedSalt, 0), true);
			if (this.conditions.size() == 1) return first;
			TypeInfo type = TypeInfos.widenUntilSame(TypeInfos.FLOAT, first.getTypeInfo());
			InsnTree[] toReverseMultiply = new InsnTree[this.conditions.size()];
			toReverseMultiply[0] = first;
			for (int index = 1; index < toReverseMultiply.length; index++) {
				type = TypeInfos.widenUntilSame(type, (toReverseMultiply[index] = this.conditions.get(index).emitChance(context, caller, Permuter.permute(seedSalt, index), true)).getTypeInfo());
			}
			for (int index = 0; index < toReverseMultiply.length; index++) {
				toReverseMultiply[index] = CastingSupport.primitiveCast(toReverseMultiply[index], type);
			}
			for (int index = 0; index < toReverseMultiply.length; index++) {
				toReverseMultiply[index] = sub(CastingSupport.dummyParser(), ldc(1.0F, type), toReverseMultiply[index]);
			}
			return sub(CastingSupport.dummyParser(), ldc(1.0F, type), mul(CastingSupport.dummyParser(), toReverseMultiply));
		}

		@Override
		public ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			ConditionTree result = this.conditions.getFirst().emitBoolean(context, caller, Permuter.permute(seedSalt, 0));
			for (int index = 0; index < this.conditions.size(); index++) {
				result = or(result, this.conditions.get(index).emitBoolean(context, caller, Permuter.permute(seedSalt, index)));
			}
			return result;
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return this.conditions.stream().flatMap(ConditionProvider::streamDirectDependencies);
		}
	}

	public static record NotConditionProvider(ConditionProvider condition) implements ConditionProvider {

		@Override
		public InsnTree emitChance(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt, boolean clamp) throws ScriptParsingException, ConstantFormatException {
			InsnTree chance = this.condition.emitChance(context, caller, seedSalt, false);
			if (chance instanceof SubtractInsnTree subtract && subtract.left.getConstantValue().isConstant() && subtract.left.getConstantValue().asDouble() == 1.0D) {
				return subtract.right;
			}
			return sub(CastingSupport.dummyParser(), ldc(1.0F, chance.getTypeInfo()), chance);
		}

		@Override
		public ConditionTree emitBoolean(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			return not(this.condition.emitBoolean(context, caller, seedSalt));
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return this.condition.streamDirectDependencies();
		}
	}
}