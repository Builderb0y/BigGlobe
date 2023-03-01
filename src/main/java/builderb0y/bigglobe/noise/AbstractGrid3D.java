package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

public abstract class AbstractGrid3D extends AbstractGrid implements Grid3D {

	/** distance between lattice points. */
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX, scaleY, scaleZ;
	/** reciprocals of {@link #scaleX}, {@link #scaleY}, and {@link #scaleZ}. */
	public final transient double rcpX, rcpY, rcpZ;

	public AbstractGrid3D(Seed salt, double amplitude, int scaleX, int scaleY, int scaleZ) {
		super(salt, amplitude);
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.rcpY = 1.0D / (this.scaleY = scaleY);
		this.rcpZ = 1.0D / (this.scaleZ = scaleZ);
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		seed ^= this.salt.value;
		int relativeX = Math.floorDiv(x, this.scaleX), fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int relativeY = Math.floorDiv(y, this.scaleY), fracY = BigGlobeMath.modulus_BP(y, this.scaleY);
		int relativeZ = Math.floorDiv(z, this.scaleZ), fracZ = BigGlobeMath.modulus_BP(z, this.scaleZ);
		//avoid interpolation where possible.
		return (
			fracX == 0
			? (
				fracY == 0
				? (
					fracZ == 0
					? this.getValue_None(seed, relativeX, relativeY, relativeZ)
					: this.getValue_Z(seed, relativeX, relativeY, relativeZ, this.fracZ(fracZ))
				)
				: (
					fracZ == 0
					? this.getValue_Y(seed, relativeX, relativeY, relativeZ, this.fracY(fracY))
					: this.getValue_YZ(seed, relativeX, relativeY, relativeZ, this.fracY(fracY), this.fracZ(fracZ))
				)
			)
			: (
				fracY == 0
				? (
					fracZ == 0
					? this.getValue_X(seed, relativeX, relativeY, relativeZ, this.fracX(fracX))
					: this.getValue_XZ(seed, relativeX, relativeY, relativeZ, this.fracX(fracX), this.fracZ(fracZ))
				)
				: (
					fracZ == 0
					? this.getValue_XY(seed, relativeX, relativeY, relativeZ, this.fracX(fracX), this.fracY(fracY))
					: this.getValue_XYZ(seed, relativeX, relativeY, relativeZ, this.fracX(fracX), this.fracY(fracY), this.fracZ(fracZ))
				)
			)
		)
		* this.amplitude;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, double[] samples, int sampleCount) {
		if (sampleCount <= 0) return;
		seed ^= this.salt.value;
		int fracY = BigGlobeMath.modulus_BP(y, this.scaleY);
		int fracZ = BigGlobeMath.modulus_BP(z, this.scaleZ);
		if (fracY == 0) {
			if (fracZ == 0) {
				this.getValuesX_None(seed, startX, y, z, samples, sampleCount);
			}
			else {
				this.getValuesX_Z(seed, startX, y, z, this.fracZ(fracZ), samples, sampleCount);
			}
		}
		else {
			if (fracZ == 0) {
				this.getValuesX_Y(seed, startX, y, z, this.fracY(fracY), samples, sampleCount);
			}
			else {
				this.getValuesX_YZ(seed, startX, y, z, this.fracY(fracY), this.fracZ(fracZ), samples, sampleCount);
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, double[] samples, int sampleCount) {
		if (sampleCount <= 0) return;
		seed ^= this.salt.value;
		int fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracZ = BigGlobeMath.modulus_BP(z, this.scaleZ);
		if (fracX == 0) {
			if (fracZ == 0) {
				this.getValuesY_None(seed, x, startY, z, samples, sampleCount);
			}
			else {
				this.getValuesY_Z(seed, x, startY, z, this.fracZ(fracZ), samples, sampleCount);
			}
		}
		else {
			if (fracZ == 0) {
				this.getValuesY_X(seed, x, startY, z, this.fracX(fracX), samples, sampleCount);
			}
			else {
				this.getValuesY_XZ(seed, x, startY, z, this.fracX(fracX), this.fracZ(fracZ), samples, sampleCount);
			}
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, double[] samples, int sampleCount) {
		if (sampleCount <= 0) return;
		seed ^= this.salt.value;
		int fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY = BigGlobeMath.modulus_BP(y, this.scaleY);
		if (fracX == 0) {
			if (fracY == 0) {
				this.getValuesZ_None(seed, x, y, startZ, samples, sampleCount);
			}
			else {
				this.getValuesZ_Y(seed, x, y, startZ, this.fracY(fracY), samples, sampleCount);
			}
		}
		else {
			if (fracY == 0) {
				this.getValuesZ_X(seed, x, y, startZ, this.fracX(fracX), samples, sampleCount);
			}
			else {
				this.getValuesZ_XY(seed, x, y, startZ, this.fracX(fracX), this.fracY(fracY), samples, sampleCount);
			}
		}
	}

	/**
	gets the value at a position where:
		x IS evenly divisible by {@link #scaleX}.
		y IS evenly divisible by {@link #scaleY}.
		z IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the y axis is NOT necessary.
		interpolation along the z axis is NOT necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is that other methods may call
	this method more than once during interpolation,
	and it is faster to multiply by {@link #amplitude}
	once at the end instead of once for every
	lattice point that needs to be interpolated.

	@param relativeX the position of the lattice point on the X axis.
	in other words, x / {@link #scaleX}.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param relativeZ the position of the lattice point on the Z axis.
	in other words, z / {@link #scaleZ}.
	*/
	public double getValue_None(long seed, int relativeX, int relativeY, int relativeZ) {
		return Permuter.toUniformDouble(Permuter.permute(seed, relativeX, relativeY, relativeZ));
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
	public abstract double getValue_X(long seed, int relativeX, int relativeY, int relativeZ, double fracX);

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
	public abstract double getValue_Y(long seed, int relativeX, int relativeY, int relativeZ, double fracY);

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
	public abstract double getValue_Z(long seed, int relativeX, int relativeY, int relativeZ, double fracZ);

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
	public abstract double getValue_XY(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracY);

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
	public abstract double getValue_XZ(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracZ);

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
	public abstract double getValue_YZ(long seed, int relativeX, int relativeY, int relativeZ, double fracY, double fracZ);

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
	public abstract double getValue_XYZ(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracY, double fracZ);

	/**
	implementation for {@link Grid3D#getBulkX(long, int, int, int, double[], int)} when:
		y IS evenly divisible by {@link #scaleY}.
		z IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the y axis is NOT necessary.
		interpolation along the z axis is NOT necessary.
	*/
	public abstract void getValuesX_None(long seed, int startX, int y, int z, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkX(long, int, int, int, double[], int)} when:
		y is NOT evenly divisible by {@link #scaleY}.
		z IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the y axis IS necessary.
		interpolation along the z axis is NOT necessary.
	*/
	public abstract void getValuesX_Y(long seed, int startX, int y, int z, double fracY, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkX(long, int, int, int, double[], int)} when:
		y IS evenly divisible by {@link #scaleY}.
		z is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the y axis is NOT necessary.
		interpolation along the z axis IS necessary.
	*/
	public abstract void getValuesX_Z(long seed, int startX, int y, int z, double fracZ, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkX(long, int, int, int, double[], int)} when:
		y is NOT evenly divisible by {@link #scaleY}.
		z is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the y axis IS necessary.
		interpolation along the z axis IS necessary.
	*/
	public abstract void getValuesX_YZ(long seed, int startX, int y, int z, double fracY, double fracZ, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkY(long, int, int, int, double[], int)} when:
		x IS evenly divisible by {@link #scaleX}.
		z IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the z axis is NOT necessary.
	*/
	public abstract void getValuesY_None(long seed, int x, int startY, int z, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkY(long, int, int, int, double[], int)} when:
		x is NOT evenly divisible by {@link #scaleX}.
		z IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the z axis is NOT necessary.
	*/
	public abstract void getValuesY_X(long seed, int x, int startY, int z, double fracX, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkY(long, int, int, int, double[], int)} when:
		x IS evenly divisible by {@link #scaleX}.
		z is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the z axis IS necessary.
	*/
	public abstract void getValuesY_Z(long seed, int x, int startY, int z, double fracZ, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkY(long, int, int, int, double[], int)} when:
		x is NOT evenly divisible by {@link #scaleX}.
		z is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the z axis IS necessary.
	*/
	public abstract void getValuesY_XZ(long seed, int x, int startY, int z, double fracX, double fracZ, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkZ(long, int, int, int, double[], int)} when:
		x IS evenly divisible by {@link #scaleX}.
		y IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the y axis is NOT necessary.
	*/
	public abstract void getValuesZ_None(long seed, int x, int y, int startZ, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkZ(long, int, int, int, double[], int)} when:
		x is NOT evenly divisible by {@link #scaleX}.
		y IS evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the y axis is NOT necessary.
	*/
	public abstract void getValuesZ_X(long seed, int x, int y, int startZ, double fracX, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkZ(long, int, int, int, double[], int)} when:
		x IS evenly divisible by {@link #scaleX}.
		y is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the y axis IS necessary.
	*/
	public abstract void getValuesZ_Y(long seed, int x, int y, int startZ, double fracY, final double[] samples, final int sampleCount);

	/**
	implementation for {@link Grid3D#getBulkZ(long, int, int, int, double[], int)} when:
		x is NOT evenly divisible by {@link #scaleX}.
		y is NOT evenly divisible by {@link #scaleZ}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the y axis IS necessary.
	*/
	public abstract void getValuesZ_XY(long seed, int x, int y, int startZ, double fracX, double fracY, final double[] samples, final int sampleCount);

	/**
	converts a position between lattice points from
	the range 0 to {@link #scaleX} to the range 0 to 1.
	subclasses may also apply {@link Interpolator#smooth(double) smoothification}.
	@param fracX the position between lattice points on the x axis.
	this value should be between 0 and {@link #scaleX}.
	*/
	public abstract double fracX(int fracX);

	/**
	converts a position between lattice points from
	the range 0 to {@link #scaleY} to the range 0 to 1.
	subclasses may also apply {@link Interpolator#smooth(double) smoothification}.
	@param fracY the position between lattice points on the y axis.
	this value should be between 0 and {@link #scaleY}.
	*/
	public abstract double fracY(int fracY);

	/**
	converts a position between lattice points from
	the range 0 to {@link #scaleZ} to the range 0 to 1.
	subclasses may also apply {@link Interpolator#smooth(double) smoothification}.
	@param fracZ the position between lattice points on the z axis.
	this value should be between 0 and {@link #scaleZ}.
	*/
	public abstract double fracZ(int fracZ);

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { scale: " + this.scaleX + ", " + this.scaleY + ", " + this.scaleZ + ", amplitude: " + this.amplitude + " }";
	}
}