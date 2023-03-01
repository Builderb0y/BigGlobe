package builderb0y.bigglobe.overriders.overworld.caves;

import builderb0y.bigglobe.math.Interpolator;

public class OceanFloorCaveExcluder implements OverworldCaveExcluder {

	public static final OceanFloorCaveExcluder INSTANCE = new OceanFloorCaveExcluder();

	@Override
	public void exclude(Context context) {
		int seaLevel = context.column.settings.height().sea_level();
		double beachY = context.column.settings.miscellaneous().beach_y();
		double doubleBeachY = Interpolator.mixLinear(seaLevel, beachY, 2.0D);
		double maxY = context.topD;
		if (maxY >= doubleBeachY) return;
		double multiplier = Interpolator.unmixSmooth(doubleBeachY, beachY, maxY);
		context.excludeSurface(multiplier);
	}
}