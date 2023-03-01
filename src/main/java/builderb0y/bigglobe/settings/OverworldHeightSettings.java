package builderb0y.bigglobe.settings;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.noise.ErosionGrid2D;
import builderb0y.bigglobe.noise.Grid2D;

public record OverworldHeightSettings(
	@UseVerifier(name = "verifyDivisibleBy16", in = OverworldHeightSettings.class, usage = MemberUsage.METHOD_IS_HANDLER)
	int min_y,
	@UseVerifier(name = "verifyDivisibleBy16", in = OverworldHeightSettings.class, usage = MemberUsage.METHOD_IS_HANDLER)
	int max_y,
	@VerifySorted(greaterThanOrEqual = "min_y", lessThanOrEqual = "max_y")
	int sea_level,
	Grid2D hilliness,
	@VerifyNullable OverworldCliffSettings cliffs,
	ErosionGrid2D[] erosion
) {

	public int y_range() {
		return this.max_y - this.min_y;
	}

	public int minYAboveBedrock() {
		return this.min_y + BigGlobeOverworldChunkGenerator.BEDROCK_HEIGHT;
	}

	public static <T_Encoded> void verifyDivisibleBy16(VerifyContext<T_Encoded, Integer> context) throws VerifyException {
		if (context.object != null && (context.object.intValue() & 15) != 0) {
			throw new VerifyException(context.pathToStringBuilder().append(" must be divisible by 16.").toString());
		}
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
		return this.cliffs != null && this.cliffs.flatness > 0.0D;
	}

	public static record OverworldCliffSettings(
		@VerifyFloatRange(min = 0.0D, minInclusive = false)
		double scale,
		Grid2D cliffiness,
		Grid2D shelf_height,
		@VerifyFloatRange(min = 0.0D, max = 1.0D) double flatness
	) {}
}