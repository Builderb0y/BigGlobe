package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

/**
this implementation picks a random number between -{@link #amplitude}
and +{@link #amplitude} at evenly-spaced lattice points.
lattice points in 1D are defined as points
where x is evenly divisible by {@link #scaleX}.
positions between lattice points have values which are
interpolated {@link Interpolator#smooth(double) smoothly}
from the values of the nearest two lattice points.
the derivative at lattice points is 0,
but the 2nd derivative will be discontinuous at lattice points.
*/
public abstract class ValueGrid1D extends AbstractGrid1D {

	public ValueGrid1D(Seed salt, double amplitude, int scaleX) {
		super(salt, amplitude, scaleX);
	}

	@Override
	public double getValue_X(long seed, int relativeX, double fracX) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX),
			this.getValue_None(seed, relativeX + 1),
			fracX
		);
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position x,
	the nearest two lattice points to x are very likely to
	be the same as the nearest two lattice points to x + 1.
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid1D#getBulkX(long, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getBulkX(long seed, int startX, double[] samples, int sampleCount) {
		if (sampleCount <= 0) return;
		seed ^= this.salt.value;
		final int scaleX = this.scaleX;
		int relativeX = Math.floorDiv(startX, scaleX);
		int fracX = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0 = this.getValue_None(seed,   relativeX);
		double value1 = this.getValue_None(seed, ++relativeX);
		double diff = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples[index] = fracX == 0 ? value0 : this.fracX(fracX) * diff + value0;
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX = 0;
				value0 = value1;
				value1 = this.getValue_None(seed, ++relativeX);
				diff = value1 - value0;
			}
		}
		this.scale(samples, sampleCount);
	}
}