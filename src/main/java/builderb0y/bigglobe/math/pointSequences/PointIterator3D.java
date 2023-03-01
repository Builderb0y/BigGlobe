package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.math.BigGlobeMath;

public interface PointIterator3D extends PointIterator {

	public abstract double x();
	public abstract double y();
	public abstract double z();

	public default int floorX() {
		return BigGlobeMath.floorI(this.x());
	}

	public default int floorY() {
		return BigGlobeMath.floorI(this.y());
	}

	public default int floorZ() {
		return BigGlobeMath.floorI(this.z());
	}
}