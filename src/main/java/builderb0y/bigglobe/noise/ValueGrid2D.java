package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

/**
this implementation picks a random number between -{@link #amplitude}
and +{@link #amplitude} at evenly-spaced lattice points.
lattice points are defined as points
where x is evenly divisible by {@link #scaleX}
and y is evenly divisible by {@link #scaleY}.
positions between lattice points have values which are
interpolated {@link Interpolator#smooth(double) smoothly}
from the values of the nearest 4 lattice points.
the derivative at lattice points is 0 along both axes,
but the 2nd derivative will be discontinuous at lattice points.
*/
public abstract class ValueGrid2D extends AbstractGrid2D {

	public ValueGrid2D(Seed salt, double amplitude, int scaleX, int scaleY) {
		super(salt, amplitude, scaleX, scaleY);
	}

	@Override
	public double getValue_X(long seed, int relativeX, int relativeY, double fracX) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX, relativeY),
			this.getValue_None(seed, relativeX + 1, relativeY),
			fracX
		);
	}

	@Override
	public double getValue_Y(long seed, int relativeX, int relativeY, double fracY) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX, relativeY),
			this.getValue_None(seed, relativeX, relativeY + 1),
			fracY
		);
	}

	@Override
	public double getValue_XY(long seed, int relativeX, int relativeY, double fracX, double fracY) {
		return Interpolator.mixLinear(
			this.getValue_X(seed, relativeX, relativeY, fracX),
			this.getValue_X(seed, relativeX, relativeY + 1, fracX),
			fracY
		);
	}

	@Override
	public void getValuesX_None(long seed, int startX, int y, double[] samples, int sampleCount) {
		int    scaleX    = this.scaleX;
		int    relativeX = Math.floorDiv(startX, scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0    = this.getValue_None(seed,   relativeX, relativeY);
		double value1    = this.getValue_None(seed, ++relativeX, relativeY);
		double diff      = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples[index] = fracX == 0 ? value0 : this.fracX(fracX) * diff + value0;
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = this.getValue_None(seed, ++relativeX, relativeY);
				diff   = value1 - value0;
			}
		}
	}

	@Override
	public void getValuesX_Y(long seed, int startX, int y, double fracY, final double[] samples, final int sampleCount) {
		int    scaleX    = this.scaleX;
		int    relativeX = Math.floorDiv(startX, scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0    = this.getValue_Y(seed,   relativeX, relativeY, fracY);
		double value1    = this.getValue_Y(seed, ++relativeX, relativeY, fracY);
		double diff      = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples[index] = fracX == 0 ? value0 : this.fracX(fracX) * diff + value0;
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = this.getValue_Y(seed, ++relativeX, relativeY, fracY);
				diff   = value1 - value0;
			}
		}
	}

	@Override
	public void getValuesY_None(long seed, int x, int startY, final double[] samples, final int sampleCount) {
		int    scaleY    = this.scaleY;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(startY, scaleY);
		int    fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0    = this.getValue_None(seed, relativeX,   relativeY);
		double value1    = this.getValue_None(seed, relativeX, ++relativeY);
		double diff      = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples[index] = fracY == 0 ? value0 : this.fracY(fracY) * diff + value0;
			if (++index >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY  = 0;
				value0 = value1;
				value1 = this.getValue_None(seed, relativeX, ++relativeY);
				diff   = value1 - value0;
			}
		}
	}

	@Override
	public void getValuesY_X(long seed, int x, int startY, double fracX, final double[] samples, final int sampleCount) {
		int    scaleY    = this.scaleY;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(startY, scaleY);
		int    fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0    = this.getValue_X(seed, relativeX,   relativeY, fracX);
		double value1    = this.getValue_X(seed, relativeX, ++relativeY, fracX);
		double diff      = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples[index] = fracY == 0 ? value0 : this.fracY(fracY) * diff + value0;
			if (++index >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY  = 0;
				value0 = value1;
				value1 = this.getValue_X(seed, relativeX, ++relativeY, fracX);
				diff   = value1 - value0;
			}
		}
	}
}