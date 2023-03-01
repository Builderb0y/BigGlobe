package builderb0y.bigglobe.math.pointSequences;

public interface BoundedPointIterator2D extends PointIterator2D, BoundedPointIterator {

	public abstract double minX();
	public abstract double maxX();
	public abstract double minY();
	public abstract double maxY();

	public default boolean contains(double x, double y) {
		return x >= this.minX() && x <= this.maxX() && y >= this.minY() && y <= this.maxY();
	}

	@Override
	public default double measure() {
		return (this.maxX() - this.minX()) * (this.maxY() - this.minY());
	}

	@Override
	public default double averageDistanceBetweenPoints() {
		return Math.sqrt(this.measure() / this.index());
	}
}