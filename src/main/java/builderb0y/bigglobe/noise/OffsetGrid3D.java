package builderb0y.bigglobe.noise;

public class OffsetGrid3D implements Grid3D {

	public final Grid3D source;
	public final int offsetX, offsetY, offsetZ;

	public OffsetGrid3D(Grid3D source, int offsetX, int offsetY, int offsetZ) {
		this.source  = source;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		return this.source.getValue(seed, x + this.offsetX, y + this.offsetY, z + this.offsetZ);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		this.source.getBulkX(seed, startX + this.offsetX, y + this.offsetY, z + this.offsetZ, samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		this.source.getBulkY(seed, x + this.offsetX, startY + this.offsetY, z + this.offsetZ, samples);
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		this.source.getBulkZ(seed, x + this.offsetX, y + this.offsetY, startZ + this.offsetZ, samples);
	}

	@Override
	public double minValue() {
		return this.source.minValue();
	}

	@Override
	public double maxValue() {
		return this.source.maxValue();
	}
}