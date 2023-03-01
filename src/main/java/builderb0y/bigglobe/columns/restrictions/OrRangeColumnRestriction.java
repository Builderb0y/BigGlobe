package builderb0y.bigglobe.columns.restrictions;

import java.util.Map;

import builderb0y.bigglobe.columns.ColumnValue;

public class OrRangeColumnRestriction extends CompactColumnRestriction {


	public OrRangeColumnRestriction(Map<ColumnValue<?>, Range> ranges) {
		super(ranges);
	}

	@Override
	public ColumnRestriction createDelegate(ColumnRestriction... restrictions) {
		return new OrColumnRestriction(restrictions);
	}
}