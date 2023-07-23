package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.math.BigGlobeMath;

public class CircularPointIterator implements PointIterator2D {

	public final BoundedPointIterator1D circumference;
	public double radius;
	public double x, y;

	public CircularPointIterator(BoundedPointIterator1D circumference, double radius) {
		if (circumference.minX() != 0.0D || circumference.maxX() != BigGlobeMath.TAU) {
			throw new IllegalArgumentException("Circumference covers wrong area: " + circumference);
		}
		this.circumference = circumference;
		this.radius = radius;
	}

	public void update() {
		this.x = Math.cos(this.circumference.x()) * this.radius;
		this.y = Math.sin(this.circumference.x()) * this.radius;
	}

	@Override
	public void next() {
		this.circumference.next();
		this.update();
	}

	@Override
	public int index() {
		return this.circumference.index();
	}

	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
}