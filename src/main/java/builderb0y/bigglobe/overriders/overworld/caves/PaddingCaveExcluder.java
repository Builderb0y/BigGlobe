package builderb0y.bigglobe.overriders.overworld.caves;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;

public class PaddingCaveExcluder implements OverworldCaveExcluder {

	public static final PaddingCaveExcluder INSTANCE = new PaddingCaveExcluder();

	@Override
	public void exclude(Context context) {
		int distance = context.column.settings.underground().caves().placement().distance;
		double progress = context.caveCell.voronoiCell.progressToEdgeD(context.column.x, context.column.z);
		for (int y = context.bottomI; y < context.topI; y++) {
			double
				width     = context.caveSettings.getWidth(context.topD, y),
				threshold = 1.0D - width / (distance * 0.5D),
				fraction  = Interpolator.unmixLinear(threshold, 1.0D, progress);
			if (fraction > 0.0D) {
				context.excludeUnchecked(y, BigGlobeMath.squareD(fraction));
			}
		}
	}
}