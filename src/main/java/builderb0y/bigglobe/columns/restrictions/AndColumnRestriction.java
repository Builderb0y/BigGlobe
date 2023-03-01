package builderb0y.bigglobe.columns.restrictions;

import builderb0y.bigglobe.columns.WorldColumn;

public class AndColumnRestriction extends CompoundColumnRestriction {

	public AndColumnRestriction(ColumnRestriction... restrictions) {
		super(restrictions);
	}

	@Override
	public double getRestriction(WorldColumn column, double y) {
		ColumnRestriction[] restrictions = this.restrictions;
		double restriction = restrictions[0].getRestriction(column, y);
		for (int index = 1, length = restrictions.length; index < length && restriction > 0.0D; index++) {
			restriction *= restrictions[index].getRestriction(column, y);
		}
		return restriction;
	}
}