package builderb0y.bigglobe.settings;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.noise.ErosionGrid2D;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;

public record OverworldHeightSettings(
	@VerifyDivisibleBy16 int min_y,
	@VerifyDivisibleBy16 int max_y,
	@VerifySorted(greaterThanOrEqual = "min_y", lessThanOrEqual = "max_y")
	int sea_level,
	Grid2D hilliness,
	@VerifyNullable OverworldCliffSettings cliffs,
	ErosionGrid2D[] erosion,
	ColumnYToDoubleScript.Holder snow_height
) {

	public int y_range() {
		return this.max_y - this.min_y;
	}

	public int minYAboveBedrock() {
		return this.min_y + BigGlobeOverworldChunkGenerator.BEDROCK_HEIGHT;
	}

	public void getErosionAndSnow(long seed, int x, int z, double sharpness, double[] out) {
		double valueSum = 0.0D, snowSum = 0.0D;
		for (ErosionGrid2D grid : this.erosion) {
			grid.getValueAndSnow(seed, x, z, sharpness, out);
			valueSum += out[0];
			snowSum  += out[1];
		}
		out[0] = valueSum;
		out[1] =  snowSum;
	}

	public boolean hasCliffs() {
		return this.cliffs != null;
	}

	public static record OverworldCliffSettings(
		@VerifyFloatRange(min = 0.0D, minInclusive = false)
		double scale,
		Grid2D cliffiness,
		Grid2D shelf_height
	) {}
}