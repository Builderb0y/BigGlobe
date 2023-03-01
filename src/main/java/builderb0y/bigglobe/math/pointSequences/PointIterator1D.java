package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.math.BigGlobeMath;

public interface PointIterator1D extends PointIterator {

	public abstract double x();

	public default int floorX() {
		return BigGlobeMath.floorI(this.x());
	}
}