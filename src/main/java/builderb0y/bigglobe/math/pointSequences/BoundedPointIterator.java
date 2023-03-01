package builderb0y.bigglobe.math.pointSequences;

public interface BoundedPointIterator extends PointIterator {

	public abstract double measure();

	public abstract double averageDistanceBetweenPoints();
}