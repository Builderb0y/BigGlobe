package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class WorleyGrid3D extends WorleyGrid implements Grid3D {

	public static final double SQRT_3 = Math.sqrt(3.0D);

	public final transient double radius;

	public WorleyGrid3D(Seed salt, int scale, double amplitude) {
		super(salt, scale, amplitude, amplitude / (squareD(scale) * 3.0D));
		this.radius = scale * SQRT_3;
	}

	public double getCenterX(long seed, int cellX, int cellY, int cellZ) {
		return (cellX + Permuter.toPositiveDouble(Permuter.permute(seed ^ 0x4A15ABD55E2B33FAL, cellX, cellY, cellZ))) * this.scale;
	}

	public double getCenterY(long seed, int cellX, int cellY, int cellZ) {
		return (cellY + Permuter.toPositiveDouble(Permuter.permute(seed ^ 0xBD18E4990F501842L, cellX, cellY, cellZ))) * this.scale;
	}

	public double getCenterZ(long seed, int cellX, int cellY, int cellZ) {
		return (cellZ + Permuter.toPositiveDouble(Permuter.permute(seed ^ 0xDB0A05170C119521L, cellX, cellY, cellZ))) * this.scale;
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		seed ^= this.salt.value;
		int minCellX = Math.floorDiv( ceilI(x - this.radius), this.scale);
		int maxCellX = Math.floorDiv(floorI(x + this.radius), this.scale);
		int minCellY = Math.floorDiv( ceilI(y - this.radius), this.scale);
		int maxCellY = Math.floorDiv(floorI(y + this.radius), this.scale);
		int minCellZ = Math.floorDiv( ceilI(z - this.radius), this.scale);
		int maxCellZ = Math.floorDiv(floorI(z + this.radius), this.scale);
		double value = Double.POSITIVE_INFINITY;
		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
				for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
					double centerX = this.getCenterX(seed, cellX, cellY, cellZ);
					double centerY = this.getCenterY(seed, cellX, cellY, cellZ);
					double centerZ = this.getCenterZ(seed, cellX, cellY, cellZ);
					value = Math.min(value, squareD(centerX - x, centerY - y, centerZ - z));
				}
			}
		}
		return value * this.rcp;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		int sampleCount = samples.length();
		seed ^= this.salt.value;
		samples.fill(Double.POSITIVE_INFINITY);
		int minCellX = Math.floorDiv( ceilI(startX - this.radius), this.scale);
		int maxCellX = Math.floorDiv(floorI(startX + sampleCount + this.radius), this.scale);
		int minCellY = Math.floorDiv( ceilI(y - this.radius), this.scale);
		int maxCellY = Math.floorDiv(floorI(y + this.radius), this.scale);
		int minCellZ = Math.floorDiv( ceilI(z - this.radius), this.scale);
		int maxCellZ = Math.floorDiv(floorI(z + this.radius), this.scale);
		double radius2 = squareD(this.radius);
		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
				for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
					double centerX = this.getCenterX(seed, cellX, cellY, cellZ);
					double centerY = this.getCenterY(seed, cellX, cellY, cellZ);
					double centerZ = this.getCenterZ(seed, cellX, cellY, cellZ);
					double yz2     = squareD(centerY - y, centerZ - z);
					int limit = startX + sampleCount - 1;
					for (int x = Math.min(floorI(centerX), limit); x >= startX; x--) {
						double distance = squareD(centerX - x) + yz2;
						if (!(distance < radius2)) break;
						int index = x - startX;
						samples.min(index, distance);
					}
					for (int x = Math.max(floorI(centerX) + 1, startX); x <= limit; x++) {
						double distance = squareD(centerX - x) + yz2;
						if (!(distance < radius2)) break;
						int index = x - startX;
						samples.min(index, distance);
					}
				}
			}
		}
		this.scale(samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		seed ^= this.salt.value;
		samples.fill(Double.POSITIVE_INFINITY);
		int minCellX = Math.floorDiv( ceilI(x - this.radius), this.scale);
		int maxCellX = Math.floorDiv(floorI(x + this.radius), this.scale);
		int minCellY = Math.floorDiv( ceilI(startY - this.radius), this.scale);
		int maxCellY = Math.floorDiv(floorI(startY + sampleCount + this.radius), this.scale);
		int minCellZ = Math.floorDiv( ceilI(z - this.radius), this.scale);
		int maxCellZ = Math.floorDiv(floorI(z + this.radius), this.scale);
		double radius2 = squareD(this.radius);
		for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
			for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
				for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
					double centerX = this.getCenterX(seed, cellX, cellY, cellZ);
					double centerY = this.getCenterY(seed, cellX, cellY, cellZ);
					double centerZ = this.getCenterZ(seed, cellX, cellY, cellZ);
					double xz2     = squareD(centerX - x, centerZ - z);
					int limit = startY + sampleCount - 1;
					for (int y = Math.min(floorI(centerY), limit); y >= startY; y--) {
						double distance = squareD(centerY - y) + xz2;
						if (!(distance < radius2)) break;
						int index = y - startY;
						samples.min(index, distance);
					}
					for (int y = Math.max(floorI(centerY) + 1, startY); y <= limit; y++) {
						double distance = squareD(centerY - y) + xz2;
						if (!(distance < radius2)) break;
						int index = y - startY;
						samples.min(index, distance);
					}
				}
			}
		}
		this.scale(samples);
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		int sampleCount = samples.length();
		seed ^= this.salt.value;
		samples.fill(Double.POSITIVE_INFINITY);
		int minCellX = Math.floorDiv( ceilI(x - this.radius), this.scale);
		int maxCellX = Math.floorDiv(floorI(x + this.radius), this.scale);
		int minCellY = Math.floorDiv( ceilI(y - this.radius), this.scale);
		int maxCellY = Math.floorDiv(floorI(y + this.radius), this.scale);
		int minCellZ = Math.floorDiv( ceilI(startZ - this.radius), this.scale);
		int maxCellZ = Math.floorDiv(floorI(startZ + sampleCount + this.radius), this.scale);
		double radius2 = squareD(this.radius);
		for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
			for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
				for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
					double centerX = this.getCenterX(seed, cellX, cellY, cellZ);
					double centerY = this.getCenterY(seed, cellX, cellY, cellZ);
					double centerZ = this.getCenterZ(seed, cellX, cellY, cellZ);
					double xy2     = squareD(centerX - x, centerY - y);
					int limit = startZ + sampleCount - 1;
					for (int z = Math.min(floorI(centerZ), limit); z >= startZ; z--) {
						double distance = squareD(centerZ - z) + xy2;
						if (!(distance < radius2)) break;
						int index = z - startZ;
						samples.min(index, distance);
					}
					for (int z = Math.max(floorI(centerZ) + 1, startZ); z <= limit; z++) {
						double distance = squareD(centerZ - z) + xy2;
						if (!(distance < radius2)) break;
						int index = z - startZ;
						samples.min(index, distance);
					}
				}
			}
		}
		this.scale(samples);
	}

	/*
	public static void main(String[] args) {
		double[] values = new double[100];
		WorleyGrid3D grid = new WorleyGrid3D(new NumberSeed(Permuter.stafford(System.currentTimeMillis())), 10, 100.0D);
		grid.getBulkZ(0L, 1000, 1000, 1000, values, values.length);
		for (double value : values) {
			System.out.println("#".repeat((int)(value)));
		}
	}
	*/
}