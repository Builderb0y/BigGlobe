package builderb0y.bigglobe.columns.restrictions;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class ConstantColumnRestriction implements ColumnRestriction {

	public final double chance;

	public ConstantColumnRestriction(double chance) {
		this.chance = chance;
	}

	@Override
	public double getRestriction(ScriptedColumn column, int y) {
		return this.chance;
	}
}