package builderb0y.bigglobe.columns.restrictions;

import java.util.function.Consumer;
import java.util.stream.Stream;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;

public class ConstantColumnRestriction implements ColumnRestriction {

	public final double chance;

	public ConstantColumnRestriction(double chance) {
		this.chance = chance;
	}

	@Override
	public double getRestriction(WorldColumn column, double y) {
		return this.chance;
	}

	@Override
	public void forEachValue(Consumer<? super ColumnValue<?>> action) {}

	@Override
	public Stream<ColumnValue<?>> getValues() {
		return Stream.empty();
	}
}