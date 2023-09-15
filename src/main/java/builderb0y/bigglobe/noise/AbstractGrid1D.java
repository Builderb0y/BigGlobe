package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

public abstract class AbstractGrid1D extends AbstractGrid implements Grid1D {

	/** distance between lattice points. */
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX;
	/** reciprocal of {@link #scaleX}. */
	public final transient double rcpX;

	public AbstractGrid1D(Seed salt, double amplitude, int scaleX) {
		super(salt, amplitude);
		this.rcpX = 1.0D / (this.scaleX = scaleX);
	}

	@Override
	public double getValue(long seed, int x) {
		seed ^= this.salt.value;
		int relativeX = Math.floorDiv(x, this.scaleX), fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		//avoid interpolation where possible.
		return (
			fracX == 0
			? this.getValue_None(seed, relativeX)
			: this.getValue_X(seed, relativeX, this.fracX(fracX))
		)
		* this.amplitude;
	}

	@Override
	public abstract void getBulkX(long seed, int startX, NumberArray samples);

	/**
	gets the value at a position where x IS evenly divisible by {@link #scaleX}.
	in this case, interpolation along the x axis is NOT necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is that other methods may call
	this method more than once during interpolation,
	and it is faster to multiply by {@link #amplitude}
	once at the end instead of once for every
	lattice point that needs to be interpolated.

	@param relativeX the position of the lattice point.
	in other words, x / {@link #scaleX}.
	*/
	public double getValue_None(long seed, int relativeX) {
		return Permuter.toUniformDouble(Permuter.permute(seed, relativeX));
	}

	/**
	gets the value at a position where x is NOT evenly divisible by {@link #scaleX}.
	in this case, interpolation along the x axis IS necessary.

	the return value of this method will always be between -1 and +1,
	regardless of what {@link #amplitude} is.
	it is the responsibility of the caller to multiply by {@link #amplitude}.
	the reason for this is to be consistent with {@link #getValue_None(long, int)}.

	@param relativeX the position of the nearest lattice point, rounded down.
	in other words, the integer part of x / {@link #scaleX}.
	@param fracX the {@link Interpolator#smooth(double) smoothened}
	position within the lattice cell. in other words,
	the fractional part of x / {@link #scaleX}.
	this value should be between 0 and 1.
	*/
	public abstract double getValue_X(long seed, int relativeX, double fracX);

	/**
	converts a position between lattice points from
	the range 0 to {@link #scaleX} to the range 0 to 1.
	subclasses may also apply {@link Interpolator#smooth(double) smoothification}.
	@param fracX the position between lattice points.
	this value should be between 0 and {@link #scaleX}.
	*/
	public abstract double fracX(int fracX);

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { scale: " + this.scaleX + ", amplitude: " + this.amplitude + " }";
	}
}