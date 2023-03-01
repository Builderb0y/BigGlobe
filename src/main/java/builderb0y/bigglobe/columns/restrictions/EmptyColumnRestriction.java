package builderb0y.bigglobe.columns.restrictions;

import java.util.function.Consumer;
import java.util.stream.Stream;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;

public class EmptyColumnRestriction implements ColumnRestriction {

	@Override
	public double getRestriction(WorldColumn column, double y) {
		return 1.0D;
	}

	@Override
	public boolean test(WorldColumn column, double y, long seed) {
		return true;
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
	public String toString() {
		return "EmptyColumnRestriction";
	}
}