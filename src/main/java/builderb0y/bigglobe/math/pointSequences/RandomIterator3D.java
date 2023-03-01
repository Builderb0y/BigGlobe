package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.noise.Permuter;

public class RandomIterator3D implements BoundedPointIterator3D, RandomIterator {

	public final double minX, minY, minZ, maxX, maxY, maxZ;
	public double x, y, z;
	public final long seed;
	public int index;

	public RandomIterator3D(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, long seed) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.seed = seed;
		this.next();
	}

	@Override
	public void next() {
		this.index++;
		this.x = Permuter.toBoundedDouble(Permuter.permute(this.seed ^ 0xCE36C4BB1E41E15BL, this.index), this.minX, this.maxX);
		this.y = Permuter.toBoundedDouble(Permuter.permute(this.seed ^ 0x2A4A9F8CCB9151A1L, this.index), this.minY, this.maxY);
		this.z = Permuter.toBoundedDouble(Permuter.permute(this.seed ^ 0xE813F5ECEB515DA0L, this.index), this.minZ, this.maxZ);
	}

	@Override public double minX() { return this.minX; }
	@Override public double minY() { return this.minY; }
	@Override public double minZ() { return this.minZ; }
	@Override public double maxX() { return this.maxX; }
	@Override public double maxY() { return this.maxY; }
	@Override public double maxZ() { return this.maxZ; }
	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
	@Override public double z() { return this.z; }
	@Override public int index() { return this.index; }
	@Override public long seed() { return this.seed; }
}