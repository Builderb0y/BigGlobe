package builderb0y.bigglobe.noise.perlin;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;

public abstract class PerlinBaseGrid2D implements Grid2D {

	public static final long
		SLOPE_X_SALT = Permuter.permute(0L, 'x'),
		SLOPE_Y_SALT = Permuter.permute(0L, 'y');

	public final Seed salt;
	public final @VerifyIntRange(min = 0, minInclusive = false) @Alias("scale") int scaleX, scaleY;
	public final transient double rcpX, rcpY;
	public final double max_slope, max_offset;

	public PerlinBaseGrid2D(Seed salt, int scaleX, int scaleY, double max_slope, double max_offset) {
		this.salt = salt;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.rcpY = 1.0D / (this.scaleY = scaleY);
		this.max_slope  = max_slope;
		this.max_offset = max_offset;
	}

	public double slopeX(long seed, int x, int y) {
		return Permuter.toUniformDouble(Permuter.permute(seed ^ SLOPE_X_SALT, x, y)) * this.max_slope;
	}

	public double slopeY(long seed, int x, int y) {
		return Permuter.toUniformDouble(Permuter.permute(seed ^ SLOPE_Y_SALT, x, y)) * this.max_slope;
	}

	public double offset(long seed, int x, int y) {
		return Permuter.toUniformDouble(Permuter.permute(seed, x, y)) * this.max_offset;
	}
}