package builderb0y.bigglobe.columns.restrictions;

import java.util.Map;

import builderb0y.bigglobe.columns.ColumnValue;

public class AndRangeColumnRestriction extends CompactColumnRestriction {


	public AndRangeColumnRestriction(Map<ColumnValue<?>, Range> ranges) {
		super(ranges);
	}

	@Override
	public ColumnRestriction createDelegate(ColumnRestriction... restrictions) {
		return new AndColumnRestriction(restrictions);
	}
}