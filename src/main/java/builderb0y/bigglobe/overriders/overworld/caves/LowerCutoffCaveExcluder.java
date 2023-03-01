package builderb0y.bigglobe.overriders.overworld.caves;

import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.math.BigGlobeMath;

public class LowerCutoffCaveExcluder implements OverworldCaveExcluder {

	public static final LowerCutoffCaveExcluder INSTANCE = new LowerCutoffCaveExcluder();

	@Override
	public void exclude(Context context) {
		double minY = Math.max(context.bottomD, context.column.settings.height().minYAboveBedrock());
		CaveCell cell = context.column.getCaveCell();
		int topY = Math.min(BigGlobeMath.floorI(minY + cell.settings.lower_width()), context.topI - 1);
		double rcpLowerWidth = 1.0D / cell.settings.lower_width();
		for (int y = topY; y >= context.bottomI; y--) {
			double above = (y - minY) * rcpLowerWidth;
			context.excludeUnchecked(y, BigGlobeMath.squareD(1.0D - above));
		}
	}
}