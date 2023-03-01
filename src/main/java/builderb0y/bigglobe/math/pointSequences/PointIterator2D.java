package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.math.BigGlobeMath;

public interface PointIterator2D extends PointIterator {

	public abstract double x();
	public abstract double y();

	public default int floorX() {
		return BigGlobeMath.floorI(this.x());
	}

	public default int floorY() {
		return BigGlobeMath.floorI(this.y());
	}
}