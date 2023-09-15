package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

/**
this implementation picks a random number between -{@link #amplitude}
and +{@link #amplitude} at evenly-spaced lattice points.
lattice points are defined as points
where x is evenly divisible by {@link #scaleX},
y is evenly divisible by {@link #scaleY},
and z is evenly divisible by {@link #scaleZ}.
positions between lattice points have values which are
interpolated {@link Interpolator#smooth(double) smoothly}
from the values of the nearest 8 lattice points.
the derivative at lattice points is 0 along all axes,
but the 2nd derivative will be discontinuous at lattice points.
*/
public abstract class ValueGrid3D extends AbstractGrid3D {

	public ValueGrid3D(Seed salt, double amplitude, int scaleX, int scaleY, int scaleZ) {
		super(salt, amplitude, scaleX, scaleY, scaleZ);
	}

	@Override
	public double getValue_X(long seed, int relativeX, int relativeY, int relativeZ, double fracX) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX,     relativeY, relativeZ),
			this.getValue_None(seed, relativeX + 1, relativeY, relativeZ),
			fracX
		);
	}

	@Override
	public double getValue_Y(long seed, int relativeX, int relativeY, int relativeZ, double fracY) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX, relativeY,     relativeZ),
			this.getValue_None(seed, relativeX, relativeY + 1, relativeZ),
			fracY
		);
	}

	@Override
	public double getValue_Z(long seed, int relativeX, int relativeY, int relativeZ, double fracZ) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX, relativeY, relativeZ),
			this.getValue_None(seed, relativeX, relativeY, relativeZ + 1),
			fracZ
		);
	}

	@Override
	public double getValue_XY(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracY) {
		return Interpolator.mixLinear(
			this.getValue_X(seed, relativeX, relativeY,     relativeZ, fracX),
			this.getValue_X(seed, relativeX, relativeY + 1, relativeZ, fracX),
			fracY
		);
	}

	@Override
	public double getValue_XZ(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracZ) {
		return Interpolator.mixLinear(
			this.getValue_X(seed, relativeX, relativeY, relativeZ,     fracX),
			this.getValue_X(seed, relativeX, relativeY, relativeZ + 1, fracX),
			fracZ
		);
	}

	@Override
	public double getValue_YZ(long seed, int relativeX, int relativeY, int relativeZ, double fracY, double fracZ) {
		return Interpolator.mixLinear(
			this.getValue_Y(seed, relativeX, relativeY, relativeZ,     fracY),
			this.getValue_Y(seed, relativeX, relativeY, relativeZ + 1, fracY),
			fracZ
		);
	}

	@Override
	public double getValue_XYZ(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracY, double fracZ) {
		return Interpolator.mixLinear(
			this.getValue_XY(seed, relativeX, relativeY, relativeZ,     fracX, fracY),
			this.getValue_XY(seed, relativeX, relativeY, relativeZ + 1, fracX, fracY),
			fracZ
		);
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x + 1, y, z).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesX_None(long seed, int startX, int y, int z, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleX      = this.scaleX;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(startX, scaleX);
		int    relativeY   = Math.floorDiv(y, this.scaleY);
		int    relativeZ   = Math.floorDiv(z, this.scaleZ);
		int    fracX       = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0      = this.getValue_None(seed,   relativeX, relativeY, relativeZ) * amplitude;
		double value1      = this.getValue_None(seed, ++relativeX, relativeY, relativeZ) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? value0 : this.fracX(fracX) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = this.getValue_None(seed, ++relativeX, relativeY, relativeZ) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x + 1, y, z).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesX_Y(long seed, int startX, int y, int z, double fracY, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleX      = this.scaleX;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(startX, scaleX);
		int    relativeY   = Math.floorDiv(y, this.scaleY);
		int    relativeZ   = Math.floorDiv(z, this.scaleZ);
		int    fracX       = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0      = this.getValue_Y(seed,   relativeX, relativeY, relativeZ, fracY) * amplitude;
		double value1      = this.getValue_Y(seed, ++relativeX, relativeY, relativeZ, fracY) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? value0 : this.fracX(fracX) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = this.getValue_Y(seed, ++relativeX, relativeY, relativeZ, fracY) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x + 1, y, z).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesX_Z(long seed, int startX, int y, int z, double fracZ, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleX      = this.scaleX;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(startX, scaleX);
		int    relativeY   = Math.floorDiv(y, this.scaleY);
		int    relativeZ   = Math.floorDiv(z, this.scaleZ);
		int    fracX       = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0      = this.getValue_Z(seed,   relativeX, relativeY, relativeZ, fracZ) * amplitude;
		double value1      = this.getValue_Z(seed, ++relativeX, relativeY, relativeZ, fracZ) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? value0 : this.fracX(fracX) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = this.getValue_Z(seed, ++relativeX, relativeY, relativeZ, fracZ) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x + 1, y, z).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesX_YZ(long seed, int startX, int y, int z, double fracY, double fracZ, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleX      = this.scaleX;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(startX, scaleX);
		int    relativeY   = Math.floorDiv(y, this.scaleY);
		int    relativeZ   = Math.floorDiv(z, this.scaleZ);
		int    fracX       = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0      = this.getValue_YZ(seed,   relativeX, relativeY, relativeZ, fracY, fracZ) * amplitude;
		double value1      = this.getValue_YZ(seed, ++relativeX, relativeY, relativeZ, fracY, fracZ) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? value0 : this.fracX(fracX) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = this.getValue_YZ(seed, ++relativeX, relativeY, relativeZ, fracY, fracZ) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x, y + 1, z).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesY_None(long seed, int x, int startY, int z, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleY      = this.scaleY;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(x, this.scaleX);
		int    relativeY   = Math.floorDiv(startY, scaleY);
		int    relativeZ   = Math.floorDiv(z, this.scaleZ);
		int    fracY       = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0      = this.getValue_None(seed, relativeX,   relativeY, relativeZ) * amplitude;
		double value1      = this.getValue_None(seed, relativeX, ++relativeY, relativeZ) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracY == 0 ? value0 : this.fracY(fracY) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY  = 0;
				value0 = value1;
				value1 = this.getValue_None(seed, relativeX, ++relativeY, relativeZ) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x, y + 1, z).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesY_X(long seed, int x, int startY, int z, double fracX, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleY      = this.scaleY;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(x, this.scaleX);
		int    relativeY   = Math.floorDiv(startY, scaleY);
		int    relativeZ   = Math.floorDiv(z, this.scaleZ);
		int    fracY       = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0      = this.getValue_X(seed, relativeX,   relativeY, relativeZ, fracX) * amplitude;
		double value1      = this.getValue_X(seed, relativeX, ++relativeY, relativeZ, fracX) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracY == 0 ? value0 : this.fracY(fracY) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY  = 0;
				value0 = value1;
				value1 = this.getValue_X(seed, relativeX, ++relativeY, relativeZ, fracX) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x, y + 1, z).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesY_Z(long seed, int x, int startY, int z, double fracZ, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleY      = this.scaleY;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(x, this.scaleX);
		int    relativeY   = Math.floorDiv(startY, scaleY);
		int    relativeZ   = Math.floorDiv(z, this.scaleZ);
		int    fracY       = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0      = this.getValue_Z(seed, relativeX,   relativeY, relativeZ, fracZ) * amplitude;
		double value1      = this.getValue_Z(seed, relativeX, ++relativeY, relativeZ, fracZ) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracY == 0 ? value0 : this.fracY(fracY) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY  = 0;
				value0 = value1;
				value1 = this.getValue_Z(seed, relativeX, ++relativeY, relativeZ, fracZ) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x, y + 1, z).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesY_XZ(long seed, int x, int startY, int z, double fracX, double fracZ, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleY      = this.scaleY;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(x, this.scaleX);
		int    relativeY   = Math.floorDiv(startY, scaleY);
		int    relativeZ   = Math.floorDiv(z, this.scaleZ);
		int    fracY       = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0      = this.getValue_XZ(seed, relativeX,   relativeY, relativeZ, fracX, fracZ) * amplitude;
		double value1      = this.getValue_XZ(seed, relativeX, ++relativeY, relativeZ, fracX, fracZ) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracY == 0 ? value0 : this.fracY(fracY) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY  = 0;
				value0 = value1;
				value1 = this.getValue_XZ(seed, relativeX, ++relativeY, relativeZ, fracX, fracZ) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x, y, z + 1).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesZ_None(long seed, int x, int y, int startZ, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleZ      = this.scaleZ;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(x, this.scaleX);
		int    relativeY   = Math.floorDiv(y, this.scaleY);
		int    relativeZ   = Math.floorDiv(startZ, scaleZ);
		int    fracZ       = BigGlobeMath.modulus_BP(startZ, scaleZ);
		double value0      = this.getValue_None(seed, relativeX, relativeY,   relativeZ) * amplitude;
		double value1      = this.getValue_None(seed, relativeX, relativeY, ++relativeZ) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracZ == 0 ? value0 : this.fracZ(fracZ) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ  = 0;
				value0 = value1;
				value1 = this.getValue_None(seed, relativeX, relativeY, ++relativeZ) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x, y, z + 1).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesZ_X(long seed, int x, int y, int startZ, double fracX, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleZ      = this.scaleZ;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(x, this.scaleX);
		int    relativeY   = Math.floorDiv(y, this.scaleY);
		int    relativeZ   = Math.floorDiv(startZ, scaleZ);
		int    fracZ       = BigGlobeMath.modulus_BP(startZ, scaleZ);
		double value0      = this.getValue_X(seed, relativeX, relativeY,   relativeZ, fracX) * amplitude;
		double value1      = this.getValue_X(seed, relativeX, relativeY, ++relativeZ, fracX) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracZ == 0 ? value0 : this.fracZ(fracZ) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ  = 0;
				value0 = value1;
				value1 = this.getValue_X(seed, relativeX, relativeY, ++relativeZ, fracX) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x, y, z + 1).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesZ_Y(long seed, int x, int y, int startZ, double fracY, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleZ      = this.scaleZ;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(x, this.scaleX);
		int    relativeY   = Math.floorDiv(y, this.scaleY);
		int    relativeZ   = Math.floorDiv(startZ, scaleZ);
		int    fracZ       = BigGlobeMath.modulus_BP(startZ, scaleZ);
		double value0      = this.getValue_Y(seed, relativeX, relativeY,   relativeZ, fracY) * amplitude;
		double value1      = this.getValue_Y(seed, relativeX, relativeY, ++relativeZ, fracY) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracZ == 0 ? value0 : this.fracZ(fracZ) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ  = 0;
				value0 = value1;
				value1 = this.getValue_Y(seed, relativeX, relativeY, ++relativeZ, fracY) * amplitude;
				diff   = value1 - value0;
			}
		}
	}

	/**
	{@inheritDoc}

	this implementation takes advantage of the fact that for any given position (x, y, z),
	the nearest 8 lattice points to (x, y, z) are very likely to
	be the same as the nearest 8 lattice points to (x, y, z + 1).
	because of this, the values associated with those
	lattice points are also going to be the same.
	so, the act of calculating those values is skipped unless
	the lattice points themselves are actually different.
	this results in much greater efficiency than the default
	implementation on {@link Grid3D#getBulkX(long, int, int, int, NumberArray)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesZ_XY(long seed, int x, int y, int startZ, double fracX, double fracY, NumberArray samples) {
		int    sampleCount = samples.length();
		int    scaleZ      = this.scaleZ;
		double amplitude   = this.amplitude;
		int    relativeX   = Math.floorDiv(x, this.scaleX);
		int    relativeY   = Math.floorDiv(y, this.scaleY);
		int    relativeZ   = Math.floorDiv(startZ, scaleZ);
		int    fracZ       = BigGlobeMath.modulus_BP(startZ, scaleZ);
		double value0      = this.getValue_XY(seed, relativeX, relativeY,   relativeZ, fracX, fracY) * amplitude;
		double value1      = this.getValue_XY(seed, relativeX, relativeY, ++relativeZ, fracX, fracY) * amplitude;
		double diff        = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracZ == 0 ? value0 : this.fracZ(fracZ) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ  = 0;
				value0 = value1;
				value1 = this.getValue_XY(seed, relativeX, relativeY, ++relativeZ, fracX, fracY) * amplitude;
				diff   = value1 - value0;
			}
		}
	}
}