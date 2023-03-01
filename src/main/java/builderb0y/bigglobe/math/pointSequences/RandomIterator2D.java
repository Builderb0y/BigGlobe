package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.noise.Permuter;

public class RandomIterator2D implements BoundedPointIterator2D, RandomIterator {

	public final double minX, minY, maxX, maxY;
	public double x, y;
	public final long seed;
	public int index;

	public RandomIterator2D(double minX, double minY, double maxX, double maxY, long seed) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.seed = seed;
		this.next();
	}

	@Override
	public void next() {
		this.index++;
		this.x = Permuter.toBoundedDouble(Permuter.permute(this.seed ^ 0x0F27519584CA4529L, this.index), this.minX, this.maxX);
		this.y = Permuter.toBoundedDouble(Permuter.permute(this.seed ^ 0xD2DF5D152E030F8EL, this.index), this.minY, this.maxY);
	}

	@Override public double minX() { return this.minX; }
	@Override public double minY() { return this.minY; }
	@Override public double maxX() { return this.maxX; }
	@Override public double maxY() { return this.maxY; }
	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
	@Override public int index() { return this.index; }
	@Override public long seed() { return this.seed; }
}