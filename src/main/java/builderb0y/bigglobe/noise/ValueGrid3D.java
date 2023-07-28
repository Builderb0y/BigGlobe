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

	/**
	gets the value at a position where:
		x is NOT evenly divisible by {@link #scaleX}.
		y IS evenly divisible by {@link #scaleY}.
		z IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the y axis is NOT necessary.
		interpolation along the z axis is NOT necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int, int)}.

	@param relativeX the position of the lattice point on the X axis.
	in other words, x / {@link #scaleX}.
	@param fracX the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the x axis.
	in other words, the fractional part of x / {@link #scaleX}.
	this value should be between 0 and 1.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param relativeZ the position of the lattice point on the Z axis.
	in other words, z / {@link #scaleZ}.
	*/
	@Override
	public double getValue_X(long seed, int relativeX, int relativeY, int relativeZ, double fracX) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX,     relativeY, relativeZ),
			this.getValue_None(seed, relativeX + 1, relativeY, relativeZ),
			fracX
		);
	}

	/**
	gets the value at a position where:
		x IS evenly divisible by {@link #scaleX}.
		y is NOT evenly divisible by {@link #scaleY}.
		z IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the y axis IS necessary.
		interpolation along the z axis is NOT necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int, int)}.

	@param relativeX the position of the lattice point on the X axis.
	in other words, x / {@link #scaleX}.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param fracY the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the y axis.
	in other words, the fractional part of y / {@link #scaleY}.
	this value should be between 0 and 1.
	@param relativeZ the position of the lattice point on the Z axis.
	in other words, z / {@link #scaleZ}.
	*/
	@Override
	public double getValue_Y(long seed, int relativeX, int relativeY, int relativeZ, double fracY) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX, relativeY,     relativeZ),
			this.getValue_None(seed, relativeX, relativeY + 1, relativeZ),
			fracY
		);
	}

	/**
	gets the value at a position where:
		x IS evenly divisible by {@link #scaleX}.
		y IS evenly divisible by {@link #scaleY}.
		z is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the y axis is NOT necessary.
		interpolation along the z axis IS necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int, int)}.

	@param relativeX the position of the lattice point on the X axis.
	in other words, x / {@link #scaleX}.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param relativeZ the position of the lattice point on the Z axis.
	in other words, z / {@link #scaleZ}.
	@param fracZ the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the z axis.
	in other words, the fractional part of z / {@link #scaleZ}.
	this value should be between 0 and 1.
	*/
	@Override
	public double getValue_Z(long seed, int relativeX, int relativeY, int relativeZ, double fracZ) {
		return Interpolator.mixLinear(
			this.getValue_None(seed, relativeX, relativeY, relativeZ),
			this.getValue_None(seed, relativeX, relativeY, relativeZ + 1),
			fracZ
		);
	}

	/**
	gets the value at a position where:
		x is NOT evenly divisible by {@link #scaleX}.
		y is NOT evenly divisible by {@link #scaleY}.
		z IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the y axis IS necessary.
		interpolation along the z axis is NOT necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int, int)}.

	@param relativeX the position of the lattice point on the X axis.
	in other words, x / {@link #scaleX}.
	@param fracX the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the x axis.
	in other words, the fractional part of x / {@link #scaleX}.
	this value should be between 0 and 1.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param fracY the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the y axis.
	in other words, the fractional part of y / {@link #scaleY}.
	this value should be between 0 and 1.
	@param relativeZ the position of the lattice point on the Z axis.
	in other words, z / {@link #scaleZ}.
	*/
	@Override
	public double getValue_XY(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracY) {
		return Interpolator.mixLinear(
			this.getValue_X(seed, relativeX, relativeY,     relativeZ, fracX),
			this.getValue_X(seed, relativeX, relativeY + 1, relativeZ, fracX),
			fracY
		);
	}

	/**
	gets the value at a position where:
		x is NOT evenly divisible by {@link #scaleX}.
		y IS evenly divisible by {@link #scaleY}.
		z is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the y axis is NOT necessary.
		interpolation along the z axis IS necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int, int)}.

	@param relativeX the position of the lattice point on the X axis.
	in other words, x / {@link #scaleX}.
	@param fracX the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the x axis.
	in other words, the fractional part of x / {@link #scaleX}.
	this value should be between 0 and 1.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param relativeZ the position of the lattice point on the Z axis.
	in other words, z / {@link #scaleZ}.
	@param fracZ the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the z axis.
	in other words, the fractional part of z / {@link #scaleZ}.
	this value should be between 0 and 1.
	*/
	@Override
	public double getValue_XZ(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracZ) {
		return Interpolator.mixLinear(
			this.getValue_X(seed, relativeX, relativeY, relativeZ,     fracX),
			this.getValue_X(seed, relativeX, relativeY, relativeZ + 1, fracX),
			fracZ
		);
	}

	/**
	gets the value at a position where:
		x IS evenly divisible by {@link #scaleX}.
		y is NOT evenly divisible by {@link #scaleY}.
		z is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the y axis IS necessary.
		interpolation along the z axis IS necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int, int)}.

	@param relativeX the position of the lattice point on the X axis.
	in other words, x / {@link #scaleX}.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param fracY the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the y axis.
	in other words, the fractional part of y / {@link #scaleY}.
	this value should be between 0 and 1.
	@param relativeZ the position of the lattice point on the Z axis.
	in other words, z / {@link #scaleZ}.
	@param fracZ the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the z axis.
	in other words, the fractional part of z / {@link #scaleZ}.
	this value should be between 0 and 1.
	*/
	@Override
	public double getValue_YZ(long seed, int relativeX, int relativeY, int relativeZ, double fracY, double fracZ) {
		return Interpolator.mixLinear(
			this.getValue_Y(seed, relativeX, relativeY, relativeZ,     fracY),
			this.getValue_Y(seed, relativeX, relativeY, relativeZ + 1, fracY),
			fracZ
		);
	}

	/**
	gets the value at a position where:
		x is NOT evenly divisible by {@link #scaleX}.
		y is NOT evenly divisible by {@link #scaleY}.
		z is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the y axis IS necessary.
		interpolation along the z axis IS necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int, int)}.

	@param relativeX the position of the lattice point on the X axis.
	in other words, x / {@link #scaleX}.
	@param fracX the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the x axis.
	in other words, the fractional part of x / {@link #scaleX}.
	this value should be between 0 and 1.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param fracY the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the y axis.
	in other words, the fractional part of y / {@link #scaleY}.
	this value should be between 0 and 1.
	@param relativeZ the position of the lattice point on the Z axis.
	in other words, z / {@link #scaleZ}.
	@param fracZ the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the z axis.
	in other words, the fractional part of z / {@link #scaleZ}.
	this value should be between 0 and 1.
	*/
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesX_None(long seed, int startX, int y, int z, final double[] samples, final int sampleCount) {
		int    scaleX    = this.scaleX;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(startX, scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    relativeZ = Math.floorDiv(z, this.scaleZ);
		int    fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0    = this.getValue_None(seed,   relativeX, relativeY, relativeZ) * amplitude;
		double value1    = this.getValue_None(seed, ++relativeX, relativeY, relativeZ) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracX == 0 ? value0 : this.fracX(fracX) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesX_Y(long seed, int startX, int y, int z, double fracY, final double[] samples, final int sampleCount) {
		int    scaleX    = this.scaleX;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(startX, scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    relativeZ = Math.floorDiv(z, this.scaleZ);
		int    fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0    = this.getValue_Y(seed,   relativeX, relativeY, relativeZ, fracY) * amplitude;
		double value1    = this.getValue_Y(seed, ++relativeX, relativeY, relativeZ, fracY) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracX == 0 ? value0 : this.fracX(fracX) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesX_Z(long seed, int startX, int y, int z, double fracZ, final double[] samples, final int sampleCount) {
		int    scaleX    = this.scaleX;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(startX, scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    relativeZ = Math.floorDiv(z, this.scaleZ);
		int    fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0    = this.getValue_Z(seed,   relativeX, relativeY, relativeZ, fracZ) * amplitude;
		double value1    = this.getValue_Z(seed, ++relativeX, relativeY, relativeZ, fracZ) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracX == 0 ? value0 : this.fracX(fracX) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesX_YZ(long seed, int startX, int y, int z, double fracY, double fracZ, final double[] samples, final int sampleCount) {
		int    scaleX    = this.scaleX;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(startX, scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    relativeZ = Math.floorDiv(z, this.scaleZ);
		int    fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		double value0    = this.getValue_YZ(seed,   relativeX, relativeY, relativeZ, fracY, fracZ) * amplitude;
		double value1    = this.getValue_YZ(seed, ++relativeX, relativeY, relativeZ, fracY, fracZ) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracX == 0 ? value0 : this.fracX(fracX) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesY_None(long seed, int x, int startY, int z, final double[] samples, final int sampleCount) {
		int    scaleY    = this.scaleY;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(startY, scaleY);
		int    relativeZ = Math.floorDiv(z, this.scaleZ);
		int    fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0    = this.getValue_None(seed, relativeX,   relativeY, relativeZ) * amplitude;
		double value1    = this.getValue_None(seed, relativeX, ++relativeY, relativeZ) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracY == 0 ? value0 : this.fracY(fracY) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesY_X(long seed, int x, int startY, int z, double fracX, final double[] samples, final int sampleCount) {
		int    scaleY    = this.scaleY;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(startY, scaleY);
		int    relativeZ = Math.floorDiv(z, this.scaleZ);
		int    fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0    = this.getValue_X(seed, relativeX,   relativeY, relativeZ, fracX) * amplitude;
		double value1    = this.getValue_X(seed, relativeX, ++relativeY, relativeZ, fracX) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracY == 0 ? value0 : this.fracY(fracY) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesY_Z(long seed, int x, int startY, int z, double fracZ, final double[] samples, final int sampleCount) {
		int    scaleY    = this.scaleY;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(startY, scaleY);
		int    relativeZ = Math.floorDiv(z, this.scaleZ);
		int    fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0    = this.getValue_Z(seed, relativeX,   relativeY, relativeZ, fracZ) * amplitude;
		double value1    = this.getValue_Z(seed, relativeX, ++relativeY, relativeZ, fracZ) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracY == 0 ? value0 : this.fracY(fracY) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesY_XZ(long seed, int x, int startY, int z, double fracX, double fracZ, final double[] samples, final int sampleCount) {
		int    scaleY    = this.scaleY;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(startY, scaleY);
		int    relativeZ = Math.floorDiv(z, this.scaleZ);
		int    fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		double value0    = this.getValue_XZ(seed, relativeX,   relativeY, relativeZ, fracX, fracZ) * amplitude;
		double value1    = this.getValue_XZ(seed, relativeX, ++relativeY, relativeZ, fracX, fracZ) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracY == 0 ? value0 : this.fracY(fracY) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesZ_None(long seed, int x, int y, int startZ, final double[] samples, final int sampleCount) {
		int    scaleZ    = this.scaleZ;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    relativeZ = Math.floorDiv(startZ, scaleZ);
		int    fracZ     = BigGlobeMath.modulus_BP(startZ, scaleZ);
		double value0    = this.getValue_None(seed, relativeX, relativeY,   relativeZ) * amplitude;
		double value1    = this.getValue_None(seed, relativeX, relativeY, ++relativeZ) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracZ == 0 ? value0 : this.fracZ(fracZ) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesZ_X(long seed, int x, int y, int startZ, double fracX, final double[] samples, final int sampleCount) {
		int    scaleZ    = this.scaleZ;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    relativeZ = Math.floorDiv(startZ, scaleZ);
		int    fracZ     = BigGlobeMath.modulus_BP(startZ, scaleZ);
		double value0    = this.getValue_X(seed, relativeX, relativeY,   relativeZ, fracX) * amplitude;
		double value1    = this.getValue_X(seed, relativeX, relativeY, ++relativeZ, fracX) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracZ == 0 ? value0 : this.fracZ(fracZ) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesZ_Y(long seed, int x, int y, int startZ, double fracY, final double[] samples, final int sampleCount) {
		int    scaleZ    = this.scaleZ;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    relativeZ = Math.floorDiv(startZ, scaleZ);
		int    fracZ     = BigGlobeMath.modulus_BP(startZ, scaleZ);
		double value0    = this.getValue_Y(seed, relativeX, relativeY,   relativeZ, fracY) * amplitude;
		double value1    = this.getValue_Y(seed, relativeX, relativeY, ++relativeZ, fracY) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracZ == 0 ? value0 : this.fracZ(fracZ) * diff + value0;
			if (++i >= sampleCount) break;
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
	implementation on {@link Grid3D#getBulkX(long, int, int, int, double[], int)}
	due to needing vastly fewer redundant calculations.
	*/
	@Override
	public void getValuesZ_XY(long seed, int x, int y, int startZ, double fracX, double fracY, final double[] samples, final int sampleCount) {
		int    scaleZ    = this.scaleZ;
		double amplitude = this.amplitude;
		int    relativeX = Math.floorDiv(x, this.scaleX);
		int    relativeY = Math.floorDiv(y, this.scaleY);
		int    relativeZ = Math.floorDiv(startZ, scaleZ);
		int    fracZ     = BigGlobeMath.modulus_BP(startZ, scaleZ);
		double value0    = this.getValue_XY(seed, relativeX, relativeY,   relativeZ, fracX, fracY) * amplitude;
		double value1    = this.getValue_XY(seed, relativeX, relativeY, ++relativeZ, fracX, fracY) * amplitude;
		double diff      = value1 - value0;
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracZ == 0 ? value0 : this.fracZ(fracZ) * diff + value0;
			if (++i >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ  = 0;
				value0 = value1;
				value1 = this.getValue_XY(seed, relativeX, relativeY, ++relativeZ, fracX, fracY) * amplitude;
				diff   = value1 - value0;
			}
		}
	}
}