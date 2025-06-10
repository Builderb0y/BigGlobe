package builderb0y.bigglobe.noise.perlin;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;

public class PerlinGrid1D implements Grid1D {

	public static final long
		SLOPE_X_SALT = Permuter.permute(0L, 'x');

	public final Seed salt;
	public final @VerifyIntRange(min = 0, minInclusive = false) @Alias("scale") int scaleX;
	public final transient double rcpX;
	public final double max_slope, max_offset;

	public PerlinGrid1D(Seed salt, int scaleX, double max_slope, double max_offset) {
		this.salt = salt;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.max_slope = max_slope;
		this.max_offset = max_offset;
	}

	@Override
	public double minValue() {
		return -(this.max_slope * 0.5D + this.max_offset);
	}

	@Override
	public double maxValue() {
		return this.max_slope * 0.5D + this.max_offset;
	}

	public double slopeX(long seed, int x) {
		return Permuter.toUniformDouble(Permuter.permute(seed ^ SLOPE_X_SALT, x)) * this.max_slope;
	}

	public double offset(long seed, int x) {
		return Permuter.toUniformDouble(Permuter.permute(seed, x)) * this.max_offset;
	}

	@Override
	public double getValue(long seed, int x) {
		seed = this.salt.xor(seed);
		int
			modX = BigGlobeMath.modulus_BP(x, this.scaleX),
			gridX0 = x - modX,
			gridX1 = gridX0 + this.scaleX;
		double
			fracX0 = modX * this.rcpX,
			fracX1 = fracX0 - 1.0D;
		return Interpolator.mixSmoothUnchecked(
			+ this.slopeX(seed, gridX0) * fracX0
			+ this.offset(seed, gridX0),
			+ this.slopeX(seed, gridX1) * fracX1
			+ this.offset(seed, gridX1),
			fracX0
		);
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX  = this.scaleX,
			modX    = BigGlobeMath.modulus_BP(startX, scaleX),
			gridX   = startX - modX;
		double
			rcpX    = this.rcpX,
			slopeX0 = this.slopeX(seed, gridX),
			offset0 = this.offset(seed, gridX),
			slopeX1 = this.slopeX(seed, gridX += scaleX),
			offset1 = this.offset(seed, gridX);
		for (int index = 0; true /* break in the middle of the loop. */;) {
			double fracX0 = modX * rcpX;
			double fracX1 = fracX0 - 1.0D;
			samples.setD(index, Interpolator.mixSmoothUnchecked(slopeX0 * fracX0 + offset0, slopeX1 * fracX1 + offset1, fracX0));
			if (++index >= sampleCount) break;
			if (++modX >= scaleX) {
				modX = 0;
				slopeX0 = slopeX1;
				offset0 = offset1;
				slopeX1 = this.slopeX(seed, gridX += scaleX);
				offset1 = this.offset(seed, gridX);
			}
		}
	}
}