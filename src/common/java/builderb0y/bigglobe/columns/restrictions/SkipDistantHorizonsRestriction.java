package builderb0y.bigglobe.columns.restrictions;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.MapData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat;

@UseCoder(name = "code", usage = MemberUsage.METHOD_IS_HANDLER)
public class SkipDistantHorizonsRestriction implements ColumnRestriction {

	public static final SkipDistantHorizonsRestriction INSTANCE = new SkipDistantHorizonsRestriction();

	public static <T_Encoded> Data code(EncodeContext<T_Encoded, SkipDistantHorizonsRestriction> context) {
		return new MapData();
	}

	public static <T_Encoded> @Nullable SkipDistantHorizonsRestriction code(DecodeContext<T_Encoded> context) {
		return context.isEmpty() ? null : INSTANCE;
	}

	@Override
	public double getRestriction(ScriptedColumn column, int y) {
		return DistantHorizonsCompat.isOnDistantHorizonThread() ? 0.0D : 1.0D;
	}

	@Override
	public boolean test(ScriptedColumn column, int y, long seed) {
		return !column.params.hints().isLod();
	}

	@Override
	public String toString() {
		return "SkipDistantHorizonsRestriction";
	}
}