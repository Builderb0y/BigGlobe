package builderb0y.bigglobe.math.pointSequences;

public interface BoundedPointIterator3D extends PointIterator3D, BoundedPointIterator {

	public abstract double minX();
	public abstract double maxX();
	public abstract double minY();
	public abstract double maxY();
	public abstract double minZ();
	public abstract double maxZ();

	public default boolean contains(double x, double y, double z) {
		return x >= this.minX() && x <= this.maxX() && y >= this.minY() && y <= this.maxY() && z >= this.minZ() && z <= this.maxZ();
	}

	@Override
	public default double measure() {
		return (this.maxX() - this.minX()) * (this.maxY() - this.minY()) * (this.maxZ() - this.minZ());
	}

	@Override
	public default double averageDistanceBetweenPoints() {
		return Math.cbrt(this.measure() / this.index());
	}
}