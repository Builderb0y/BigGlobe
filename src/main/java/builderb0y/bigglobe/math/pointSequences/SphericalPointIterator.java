package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.scripting.environments.MathScriptEnvironment;

public class SphericalPointIterator implements PointIterator3D {

	public final PointIterator2D surface;
	public double radius;
	public double x, y, z;

	public SphericalPointIterator(BoundedPointIterator2D surface, double radius) {
		if (surface.minX() != -1.0D || surface.maxX() != 1.0D || surface.minY() != 0.0D || surface.maxY() != BigGlobeMath.TAU) {
			throw new IllegalArgumentException("Surface covers wrong area: " + surface);
		}
		this.surface = surface;
		this.radius = radius;
		this.update();
	}

	public static SphericalPointIterator additiveRecurrence(double startX, double startY, double radius) {
		return new SphericalPointIterator(
			new AdditiveRecurrenceIterator2D(-1.0D, 0.0D, 1.0D, BigGlobeMath.TAU, startX, startY),
			radius
		);
	}

	public static SphericalPointIterator halton(int index, double radius) {
		return new SphericalPointIterator(
			new HaltonIterator2D(-1.0D, 0.0D, 1.0D, BigGlobeMath.TAU, index),
			radius
		);
	}

	public static SphericalPointIterator random(long seed, double radius) {
		return new SphericalPointIterator(
			new RandomIterator2D(-1.0D, 0.0D, 1.0D, BigGlobeMath.TAU, seed),
			radius
		);
	}

	public void update() {
		double x = this.surface.x();
		double y = this.surface.y();
		double r = Math.sqrt(MathScriptEnvironment.max(1.0D - x * x, 0.0D)) * this.radius;
		this.x = Math.cos(y) * r;
		this.y = Math.sin(y) * r;
		this.z = x * this.radius;
	}

	@Override
	public void next() {
		this.surface.next();
		this.update();
	}

	@Override
	public int index() {
		return this.surface.index();
	}

	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
	@Override public double z() { return this.z; }
}