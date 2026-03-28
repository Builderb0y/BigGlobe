package builderb0y.bigglobe.noise.perlin;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.settings.Seed;

public class PerlinDerivativeXGrid1D extends PerlinBaseGrid1D {

	public PerlinDerivativeXGrid1D(Seed salt, int scaleX, double max_slope, double max_offset) {
		super(salt, scaleX, max_slope, max_offset);
	}

	@Override
	public double minValue() {
		throw new UnsupportedOperationException("I don't know how to find the local minimum of the derivative of perlin noise.");
	}

	@Override
	public double maxValue() {
		throw new UnsupportedOperationException("I don't know how to find the local maximum of the derivative of perlin noise.");
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
			fracX1 = fracX0 - 1.0D,
			slopeX0 = this.slopeX(seed, gridX0),
			slopeX1 = this.slopeX(seed, gridX1),
			offset0 = this.offset(seed, gridX0),
			offset1 = this.offset(seed, gridX1);
		return Interpolator.dMixPerlin(
			slopeX0 * fracX0 + offset0,
			slopeX1 * fracX1 + offset1,
			slopeX0,
			slopeX1,
			fracX0
		) * this.rcpX;
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX = this.scaleX,
			modX = BigGlobeMath.modulus_BP(startX, scaleX),
			gridX = startX - modX;
		double
			rcpX = this.rcpX,
			slopeX0 = this.slopeX(seed, gridX),
			offset0 = this.offset(seed, gridX),
			slopeX1 = this.slopeX(seed, gridX += scaleX),
			offset1 = this.offset(seed, gridX);
		for (int index = 0; true /* break in the middle of the loop. */; ) {
			double fracX0 = modX * rcpX;
			double fracX1 = fracX0 - 1.0D;
			samples.setD(
				index,
				Interpolator.dMixPerlin(
					slopeX0 * fracX0 + offset0,
					slopeX1 * fracX1 + offset1,
					slopeX0,
					slopeX1,
					fracX0
				) * rcpX
			);
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