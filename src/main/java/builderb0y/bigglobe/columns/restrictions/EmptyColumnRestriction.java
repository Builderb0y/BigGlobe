package builderb0y.bigglobe.columns.restrictions;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class EmptyColumnRestriction implements ColumnRestriction {

	@Override
	public double getRestriction(ScriptedColumn column, int y) {
		return 1.0D;
	}

	@Override
	public boolean test(ScriptedColumn column, int y, long seed) {
		return true;
	}

	@Override
	public String toString() {
		return "EmptyColumnRestriction";
	}
}