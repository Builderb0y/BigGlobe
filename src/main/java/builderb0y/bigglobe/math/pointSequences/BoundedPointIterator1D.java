package builderb0y.bigglobe.math.pointSequences;

public interface BoundedPointIterator1D extends PointIterator1D, BoundedPointIterator {

	public abstract double minX();
	public abstract double maxX();

	public default boolean contains(double x) {
		return x >= this.minX() && x <= this.maxX();
	}

	@Override
	public default double measure() {
		return this.maxX() - this.minX();
	}

	@Override
	public default double averageDistanceBetweenPoints() {
		return this.measure() / this.index();
	}
}