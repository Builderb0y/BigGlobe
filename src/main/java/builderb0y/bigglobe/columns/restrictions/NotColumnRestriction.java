package builderb0y.bigglobe.columns.restrictions;

import java.util.function.Consumer;
import java.util.stream.Stream;

import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;

public class NotColumnRestriction implements ColumnRestriction {

	public final ColumnRestriction restriction;

	public NotColumnRestriction(ColumnRestriction restriction) {
		this.restriction = restriction;
	}

	@Override
	public double getRestriction(WorldColumn column, double y) {
		return 1.0D - this.restriction.getRestriction(column, y);
	}

	/*
	@Override
	public boolean dependsOnY(WorldColumn column) {
		return this.restriction.dependsOnY(column);
	}
	*/

	@Override
	public void forEachValue(Consumer<? super ColumnValue<?>> action) {
		this.restriction.forEachValue(action);
	}

	@Override
	public Stream<ColumnValue<?>> getValues() {
		return this.restriction.getValues();
	}

	@Override
	public int hashCode() {
		return -this.restriction.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof NotColumnRestriction that &&
			this.restriction.equals(that.restriction)
		);
	}

	@Override
	public String toString() {
		return TypeFormatter.getSimpleClassName(this.getClass()) + '(' + this.restriction + ')';
	}
}