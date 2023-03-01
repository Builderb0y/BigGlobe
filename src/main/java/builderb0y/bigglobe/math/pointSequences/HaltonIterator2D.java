package builderb0y.bigglobe.math.pointSequences;

public class HaltonIterator2D implements BoundedPointIterator2D, HaltonIterator {

	public int index;
	public final int offset;
	public final double minX, minY, maxX, maxY;
	public double x, y;

	public HaltonIterator2D(double minX, double minY, double maxX, double maxY, int offset) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.offset = offset;
		this.next();
	}

	@Override
	public void next() {
		this.index++;
		this.x = this.computePosition(5, 2, this.minX, this.maxX);
		this.y = this.computePosition(5, 3, this.minY, this.maxY);
	}

	@Override public double minX() { return this.minX; }
	@Override public double maxX() { return this.maxX; }
	@Override public double minY() { return this.minY; }
	@Override public double maxY() { return this.maxY; }
	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
	@Override public int index() { return this.index; }
	@Override public int offset() { return this.offset; }
}