package builderb0y.bigglobe.columns.restrictions;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.VerifySizeRange;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;

public abstract class CompactColumnRestriction implements ColumnRestriction {

	public static record Range(
		@VerifyNullable Double min,
		@VerifyNullable Double mid,
		@VerifyNullable Double max,
		@DefaultBoolean(true) boolean smooth
	) {}

	public final @EncodeInline @VerifySizeRange(min = 2) Map<ColumnValue<?>, AndRangeColumnRestriction.Range> ranges;
	public final transient ColumnRestriction delegate;

	public CompactColumnRestriction(Map<ColumnValue<?>, AndRangeColumnRestriction.Range> ranges) {
		this.ranges = ranges;
		this.delegate = new AndColumnRestriction(
			ranges
			.entrySet()
			.stream()
			.map(entry -> new RangeColumnRestriction(
				entry.getKey(),
				entry.getValue().min(),
				entry.getValue().mid(),
				entry.getValue().max(),
				entry.getValue().smooth()
			))
			.toArray(ColumnRestriction[]::new)
		);
	}

	public abstract ColumnRestriction createDelegate(ColumnRestriction... restrictions);

	@Override
	public double getRestriction(WorldColumn column, double y) {
		return this.delegate.getRestriction(column, y);
	}

	/*
	@Override
	public boolean dependsOnY(WorldColumn column) {
		return this.delegate.dependsOnY(column);
	}
	*/

	@Override
	public void forEachValue(Consumer<? super ColumnValue<?>> action) {
		this.delegate.forEachValue(action);
	}

	@Override
	public Stream<ColumnValue<?>> getValues() {
		return this.delegate.getValues();
	}
}