package builderb0y.bigglobe.columns.restrictions;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;

@UseCoder(name = "code", usage = MemberUsage.METHOD_IS_HANDLER)
public class SkipDistantHorizonsRestriction implements ColumnRestriction {

	public static final SkipDistantHorizonsRestriction INSTANCE = new SkipDistantHorizonsRestriction();

	public static <T_Encoded> T_Encoded code(EncodeContext<T_Encoded, SkipDistantHorizonsRestriction> context) {
		return context.emptyMap();
	}

	public static <T_Encoded> @Nullable SkipDistantHorizonsRestriction code(DecodeContext<T_Encoded> context) {
		return context.isEmpty() ? null : INSTANCE;
	}

	@Override
	public double getRestriction(WorldColumn column, double y) {
		return DistantHorizonsCompat.isOnDistantHorizonThread() ? 0.0D : 1.0D;
	}

	/*
	@Override
	public boolean dependsOnY(WorldColumn column) {
		return false;
	}
	*/

	@Override
	public void forEachValue(Consumer<? super ColumnValue<?>> action) {}

	@Override
	public Stream<ColumnValue<?>> getValues() {
		return Stream.empty();
	}

	@Override
	public boolean test(WorldColumn column, double y, long seed) {
		return !DistantHorizonsCompat.isOnDistantHorizonThread();
	}

	@Override
	public String toString() {
		return "SkipDistantHorizonsRestriction";
	}
}