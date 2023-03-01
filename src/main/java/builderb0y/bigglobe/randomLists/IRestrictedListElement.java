package builderb0y.bigglobe.randomLists;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;

public interface IRestrictedListElement extends IWeightedListElement {

	public abstract ColumnRestriction getRestrictions();

	public default double getRestrictedWeight(WorldColumn column, double y) {
		return this.getWeight() * this.getRestrictions().getRestriction(column, y);
	}
}