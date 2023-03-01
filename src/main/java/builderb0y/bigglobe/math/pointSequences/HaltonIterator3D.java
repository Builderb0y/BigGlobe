package builderb0y.bigglobe.math.pointSequences;

public class HaltonIterator3D implements BoundedPointIterator3D, HaltonIterator {

	public int index;
	public final int offset;
	public final double minX, minY, minZ, maxX, maxY, maxZ;
	public double x, y, z;

	public HaltonIterator3D(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int offset) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.offset = offset;
		this.next();
	}

	@Override
	public void next() {
		this.index++;
		this.x = this.computePosition(7, 2, this.minX, this.maxX);
		this.y = this.computePosition(7, 3, this.minY, this.maxY);
		this.z = this.computePosition(7, 5, this.minZ, this.maxZ);
	}

	@Override public double minX() { return this.minX; }
	@Override public double maxX() { return this.maxX; }
	@Override public double minZ() { return this.minZ; }
	@Override public double minY() { return this.minY; }
	@Override public double maxY() { return this.maxY; }
	@Override public double maxZ() { return this.maxZ; }
	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
	@Override public double z() { return this.z; }
	@Override public int index() { return this.index; }
	@Override public int offset() { return this.offset; }
}