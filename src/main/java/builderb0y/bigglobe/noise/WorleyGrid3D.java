package builderb0y.bigglobe.noise;

import java.util.Arrays;

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
	public void getBulkX(long seed, int startX, int y, int z, double[] samples, int sampleCount) {
		seed ^= this.salt.value;
		Arrays.fill(samples, 0, sampleCount, Double.POSITIVE_INFINITY);
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
					double offset  = Math.sqrt(radius2 - yz2);
					if (offset > 0.0D) {
						int minX = Math.max( ceilI(centerX - offset), startX);
						int maxX = Math.min(floorI(centerX + offset), startX + sampleCount - 1);
						for (int x = minX; x <= maxX; x++) {
							int index = x - startX;
							samples[index] = Math.min(samples[index], squareD(centerX - x) + yz2);
						}
					}
				}
			}
		}
		for (int index = 0; index < sampleCount; index++) {
			samples[index] *= this.rcp;
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, double[] samples, int sampleCount) {
		seed ^= this.salt.value;
		Arrays.fill(samples, 0, sampleCount, Double.POSITIVE_INFINITY);
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
					double offset  = Math.sqrt(radius2 - xz2);
					if (offset > 0.0D) {
						int minY = Math.max( ceilI(centerY - offset), startY);
						int maxY = Math.min(floorI(centerY + offset), startY + sampleCount - 1);
						for (int y = minY; y <= maxY; y++) {
							int index = y - startY;
							samples[index] = Math.min(samples[index], squareD(centerY - y) + xz2);
						}
					}
				}
			}
		}
		for (int index = 0; index < sampleCount; index++) {
			samples[index] *= this.rcp;
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, double[] samples, int sampleCount) {
		seed ^= this.salt.value;
		Arrays.fill(samples, 0, sampleCount, Double.POSITIVE_INFINITY);
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
					double offset  = Math.sqrt(radius2 - xy2);
					if (offset > 0.0D) {
						int minZ = Math.max( ceilI(centerZ - offset), startZ);
						int maxZ = Math.min(floorI(centerZ + offset), startZ + sampleCount - 1);
						for (int z = minZ; z <= maxZ; z++) {
							int index = z - startZ;
							samples[index] = Math.min(samples[index], squareD(centerZ - z) + xy2);
						}
					}
				}
			}
		}
		for (int index = 0; index < sampleCount; index++) {
			samples[index] *= this.rcp;
		}
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