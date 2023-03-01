package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.math.Interpolator;

public class AdditiveRecurrenceIterator1D implements BoundedPointIterator1D, AdditiveRecurrenceIterator {

	public static final double
		GOLDEN_RATIO_1D = 1.6180339887498948482D, //positive real solution to x^2 - x - 1 = 0
		ADD1 = 1.0D / GOLDEN_RATIO_1D;

	public int index;
	public final double minX, maxX;
	public final double offsetX;
	public double x;

	public AdditiveRecurrenceIterator1D(double minX, double maxX, double offsetX) {
		this.minX = minX;
		this.maxX = maxX;
		this.offsetX = offsetX;
		this.next();
	}

	@Override
	@SuppressWarnings("UseOfRemainderOperator")
	public void next() {
		this.index++;
		this.x = Interpolator.mixLinear(this.minX, this.maxX, (this.index * ADD1 + this.offsetX) % 1.0D);
	}

	@Override public double minX() { return this.minX; }
	@Override public double maxX() { return this.maxX; }
	@Override public double x() { return this.x; }
	@Override public int index() { return this.index; }
}