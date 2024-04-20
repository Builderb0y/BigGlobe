package builderb0y.bigglobe.columns.restrictions;

import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class NotColumnRestriction implements ColumnRestriction {

	public final ColumnRestriction restriction;

	public NotColumnRestriction(ColumnRestriction restriction) {
		this.restriction = restriction;
	}

	@Override
	public double getRestriction(ScriptedColumn column, int y) {
		return 1.0D - this.restriction.getRestriction(column, y);
	}

	@Override
	public int hashCode() {
		return ~this.restriction.hashCode();
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