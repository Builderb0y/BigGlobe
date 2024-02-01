package builderb0y.bigglobe.columns.restrictions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface ColumnRestriction extends CoderRegistryTyped<ColumnRestriction> {

	public static final ColumnRestriction EMPTY = new EmptyColumnRestriction();
	public static final CoderRegistry<ColumnRestriction> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("column_restriction")) {

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ColumnRestriction> context) throws EncodeException {
			return context.input == null || context.input == EMPTY ? context.empty() : super.encode(context);
		}

		@Override
		public @Nullable <T_Encoded> ColumnRestriction decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			return context.isEmpty() ? EMPTY : super.decode(context);
		}
	};
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("constant"),                   ConstantColumnRestriction.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("threshold"),                 ThresholdColumnRestriction.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("range"),                         RangeColumnRestriction.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("and"),                             AndColumnRestriction.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("or"),                               OrColumnRestriction.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("not"),                             NotColumnRestriction.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("skip_distant_horizons"), SkipDistantHorizonsRestriction.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("script"),                       ScriptColumnRestriction.class);
	}};

	public abstract double getRestriction(ScriptedColumn column, int y);

	public default boolean test(ScriptedColumn column, int y, long seed) {
		double restriction = this.getRestriction(column, y);
		if (!(restriction > 0.0D)) return false;
		if (restriction >= 1.0D) return true;
		return Permuter.toPositiveDouble(Permuter.permute(seed, column.x, BigGlobeMath.floorI(y), column.z)) < restriction;
	}
}