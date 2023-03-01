package builderb0y.bigglobe.math.pointSequences;

public class HaltonIterator1D implements BoundedPointIterator1D, HaltonIterator {

	public int index;
	public final int offset;
	public final double minX, maxX;
	public double x;

	public HaltonIterator1D(double minX, double maxX, int offset) {
		this.minX = minX;
		this.maxX = maxX;
		this.offset = offset;
		this.next();
	}

	@Override
	public void next() {
		this.index++;
		this.x = this.computePosition(3, 2, this.minX, this.maxX);
	}

	@Override public double minX() { return this.minX; }
	@Override public double maxX() { return this.maxX; }
	@Override public double x() { return this.x; }
	@Override public int index() { return this.index; }
	@Override public int offset() { return this.offset; }
}