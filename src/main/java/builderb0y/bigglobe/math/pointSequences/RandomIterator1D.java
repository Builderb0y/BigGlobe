package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.noise.Permuter;

public class RandomIterator1D implements BoundedPointIterator1D, RandomIterator {

	public final double minX, maxX;
	public double x;
	public final long seed;
	public int index;

	public RandomIterator1D(double minX, double maxX, long seed) {
		this.minX = minX;
		this.maxX = maxX;
		this.seed = seed;
		this.next();
	}

	@Override
	public void next() {
		this.index++;
		this.x = Permuter.toBoundedDouble(Permuter.permute(this.seed ^ 0xE8DCF4579E5CF66DL, + this.index), this.minX, this.maxX);
	}

	@Override public double minX() { return this.minX; }
	@Override public double maxX() { return this.maxX; }
	@Override public double x() { return this.x; }
	@Override public int index() { return this.index; }
	@Override public long seed() { return this.seed; }
}