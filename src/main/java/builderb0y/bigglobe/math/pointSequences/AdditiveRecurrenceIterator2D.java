package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.math.Interpolator;

public class AdditiveRecurrenceIterator2D implements BoundedPointIterator2D, AdditiveRecurrenceIterator {

	public static final double
		GOLDEN_RATIO_2D = 1.32471795724474602596D, //positive real solution to x^3 - x - 1 = 0
		ADD1 = 1.0D / GOLDEN_RATIO_2D,
		ADD2 = 1.0D / (GOLDEN_RATIO_2D * GOLDEN_RATIO_2D);

	public int index;
	public final double minX, minY, maxX, maxY;
	public final double offsetX, offsetY;
	public double x, y;

	public AdditiveRecurrenceIterator2D(
		double minX, double minY,
		double maxX, double maxY,
		double offsetX, double offsetY
	) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.next();
	}

	@Override
	@SuppressWarnings("UseOfRemainderOperator")
	public void next() {
		this.index++;
		this.x = Interpolator.mixLinear(this.minX, this.maxX, (this.index * ADD1 + this.offsetX) % 1.0D);
		this.y = Interpolator.mixLinear(this.minY, this.maxY, (this.index * ADD2 + this.offsetY) % 1.0D);
	}

	@Override public double minX() { return this.minX; }
	@Override public double maxX() { return this.maxX; }
	@Override public double minY() { return this.minY; }
	@Override public double maxY() { return this.maxY; }
	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
	@Override public int index() { return this.index; }
}