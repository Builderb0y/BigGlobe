package builderb0y.bigglobe.randomLists;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public interface IRestrictedListElement extends IWeightedListElement {

	public abstract ColumnRestriction getRestrictions();

	public default double getRestrictedWeight(ScriptedColumn column, int y) {
		return this.getWeight() * this.getRestrictions().getRestriction(column, y);
	}
}