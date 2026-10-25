package builderb0y.bigglobe.noise.perlin;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;

public abstract class PerlinBaseGrid1D implements Grid1D {

	public static final long
		SLOPE_X_SALT = Permuter.permute(0L, 'x');

	public final Seed salt;
	public final @VerifyIntRange(min = 0, minInclusive = false)
	@Alias("scale") int scaleX;
	public final transient double rcpX;
	public final double max_slope, max_offset;

	public PerlinBaseGrid1D(Seed salt, int scaleX, double max_slope, double max_offset) {
		this.salt = salt;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.max_slope = max_slope;
		this.max_offset = max_offset;
	}

	public double slopeX(long seed, int x) {
		return Permuter.toUniformDouble(Permuter.permute(seed ^ SLOPE_X_SALT, x)) * this.max_slope;
	}

	public double offset(long seed, int x) {
		return Permuter.toUniformDouble(Permuter.permute(seed, x)) * this.max_offset;
	}
}