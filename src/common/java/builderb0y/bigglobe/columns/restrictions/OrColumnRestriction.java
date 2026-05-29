package builderb0y.bigglobe.columns.restrictions;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.Interpolator;

public class OrColumnRestriction extends CompoundColumnRestriction {

	public OrColumnRestriction(ColumnRestriction... restrictions) {
		super(restrictions);
	}

	@Override
	public double getRestriction(ScriptedColumn column, int y) {
		ColumnRestriction[] restrictions = this.restrictions;
		double restriction = restrictions[0].getRestriction(column, y);
		for (int index = 1, length = restrictions.length; index < length && restriction < 1.0D; index++) {
			restriction = Interpolator.mixLinear(restriction, 1.0D, restrictions[index].getRestriction(column, y));
		}
		return restriction;
	}
}