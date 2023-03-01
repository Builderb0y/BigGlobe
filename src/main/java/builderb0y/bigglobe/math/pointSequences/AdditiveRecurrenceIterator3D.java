package builderb0y.bigglobe.math.pointSequences;

import builderb0y.bigglobe.math.Interpolator;

public class AdditiveRecurrenceIterator3D implements BoundedPointIterator3D, AdditiveRecurrenceIterator {

	public static final double
		GOLDEN_RATIO_3D = 1.22074408460575947536D, //positive real solution to x^4 - x - 1 = 0
		ADD1 = 1.0D / GOLDEN_RATIO_3D,
		ADD2 = 1.0D / (GOLDEN_RATIO_3D * GOLDEN_RATIO_3D),
		ADD3 = 1.0D / (GOLDEN_RATIO_3D * GOLDEN_RATIO_3D * GOLDEN_RATIO_3D);

	public int index;
	public final double minX, minY, minZ, maxX, maxY, maxZ;
	public final double offsetX, offsetY, offsetZ;
	public double x, y, z;

	public AdditiveRecurrenceIterator3D(
		double minX, double minY, double minZ,
		double maxX, double maxY, double maxZ,
		double offsetX, double offsetY, double offsetZ
	) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		this.next();
	}

	@Override
	@SuppressWarnings("UseOfRemainderOperator")
	public void next() {
		this.index++;
		this.x = Interpolator.mixLinear(this.minX, this.maxX, (this.index * ADD1 + this.offsetX) % 1.0D);
		this.y = Interpolator.mixLinear(this.minY, this.maxY, (this.index * ADD2 + this.offsetY) % 1.0D);
		this.z = Interpolator.mixLinear(this.minZ, this.maxZ, (this.index * ADD3 + this.offsetZ) % 1.0D);
	}

	@Override public double minX() { return this.minX; }
	@Override public double maxX() { return this.maxX; }
	@Override public double minY() { return this.minY; }
	@Override public double maxY() { return this.maxY; }
	@Override public double minZ() { return this.minZ; }
	@Override public double maxZ() { return this.maxZ; }
	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
	@Override public double z() { return this.z; }
	@Override public int index() { return this.index; }
}