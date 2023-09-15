package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

public abstract class AbstractGrid2D extends AbstractGrid implements Grid2D {

	/** distance between lattice points. */
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX, scaleY;
	/** reciprocals of {@link #scaleX} and {@link #scaleY}. */
	public final transient double rcpX, rcpY;

	public AbstractGrid2D(Seed salt, double amplitude, int scaleX, int scaleY) {
		super(salt, amplitude);
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.rcpY = 1.0D / (this.scaleY = scaleY);
	}

	@Override
	public double getValue(long seed, int x, int y) {
		seed ^= this.salt.value;
		int relativeX = Math.floorDiv(x, this.scaleX), fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int relativeY = Math.floorDiv(y, this.scaleY), fracY = BigGlobeMath.modulus_BP(y, this.scaleY);
		//avoid interpolation where possible.
		return (
			fracX == 0
			? (
				fracY == 0
				? this.getValue_None(seed, relativeX, relativeY)
				: this.getValue_Y   (seed, relativeX, relativeY, this.fracY(fracY))
			)
			: (
				fracY == 0
				? this.getValue_X (seed, relativeX, relativeY, this.fracX(fracX))
				: this.getValue_XY(seed, relativeX, relativeY, this.fracX(fracX), this.fracY(fracY))
			)
		)
		* this.amplitude;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		if (samples.length() <= 0) return;
		seed ^= this.salt.value;
		int fracY = BigGlobeMath.modulus_BP(y, this.scaleY);
		if (fracY == 0) {
			this.getValuesX_None(seed, startX, y, samples);
		}
		else {
			this.getValuesX_Y(seed, startX, y, this.fracY(fracY), samples);
		}
		this.scale(samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		if (samples.length() <= 0) return;
		seed ^= this.salt.value;
		int fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		if (fracX == 0) {
			this.getValuesY_None(seed, x, startY, samples);
		}
		else {
			this.getValuesY_X(seed, x, startY, this.fracX(fracX), samples);
		}
		this.scale(samples);
	}

	/**
	implementation for {@link #getBulkX(long, int, int, NumberArray)}
	when y IS evenly divisible by {@link #scaleY}.
	in this case, interpolation along the y axis is NOT necessary.
	*/
	public abstract void getValuesX_None(long seed, int startX, int y, NumberArray samples);

	/**
	implementation for {@link #getBulkX(long, int, int, NumberArray)}
	when y is NOT evenly divisible by {@link #scaleY}.
	in this case, interpolation along the y axis IS necessary.
	*/
	public abstract void getValuesX_Y(long seed, int startX, int y, double fracY, NumberArray samples);

	/**
	implementation for {@link #getBulkY(long, int, int, NumberArray)}
	when x IS evenly divisible by {@link #scaleX}.
	in this case, interpolation along the x axis is NOT necessary.
	*/
	public abstract void getValuesY_None(long seed, int x, int startY, NumberArray samples);

	/**
	implementation for {@link #getBulkY(long, int, int, NumberArray)}
	when x is NOT evenly divisible by {@link #scaleX}.
	in this case, interpolation along the x axis IS necessary.
	*/
	public abstract void getValuesY_X(long seed, int x, int startY, double fracX, NumberArray samples);

	/**
	gets the value at a position where:
		x IS evenly divisible by {@link #scaleX}.
		y IS evenly divisible by {@link #scaleY}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the y axis is NOT necessary.

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
	*/
	public double getValue_None(long seed, int relativeX, int relativeY) {
		return Permuter.toUniformDouble(Permuter.permute(seed, relativeX, relativeY));
	}

	/**
	gets the value at a position where:
		x is NOT evenly divisible by {@link #scaleX}.
		y IS evenly divisible by {@link #scaleY}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the y axis is NOT necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int)}.

	@param relativeX the position of the lattice point on the X axis, rounded down.
	in other words, the integer part of x / {@link #scaleX}.
	@param fracX the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the x axis.
	in other words, the fractional part of x / {@link #scaleX}.
	this value should be between 0 and 1.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	*/
	public abstract double getValue_X(long seed, int relativeX, int relativeY, double fracX);

	/**
	gets the value at a position where:
		x IS evenly divisible by {@link #scaleX}.
		y is NOT evenly divisible by {@link #scaleY}.
	in this case:
		interpolation along the x axis is NOT necessary.
		interpolation along the y axis IS necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int)}.

	@param relativeX the position of the lattice point on the X axis, rounded down.
	in other words, the integer part of x / {@link #scaleX}.
	@param relativeY the position of the lattice point on the Y axis.
	in other words, y / {@link #scaleY}.
	@param fracY the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell on the y axis.
	in other words, the fractional part of y / {@link #scaleY}.
	this value should be between 0 and 1.
	*/
	public abstract double getValue_Y(long seed, int relativeX, int relativeY, double fracY);

	/**
	gets the value at a position where:
		x is NOT evenly divisible by {@link #scaleX}.
		y is NOT evenly divisible by {@link #scaleY}.
	in this case:
		interpolation along the x axis IS necessary.
		interpolation along the y axis IS necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int, int)}.

	@param relativeX the position of the lattice point on the X axis, rounded down.
	in other words, the integer part of x / {@link #scaleX}.
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
	*/
	public abstract double getValue_XY(long seed, int relativeX, int relativeY, double fracX, double fracY);

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

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { scale: " + this.scaleX + ", " + this.scaleY + ", amplitude: " + this.amplitude + " }";
	}
}