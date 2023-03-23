package builderb0y.bigglobe.noise;

import java.util.Arrays;

import builderb0y.bigglobe.settings.Seed;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class WorleyGrid2D extends WorleyGrid implements Grid2D {

	public static final double SQRT_2 = Math.sqrt(2.0D);

	public final transient double radius;

	public WorleyGrid2D(Seed salt, int scale, double amplitude) {
		super(salt, scale, amplitude, amplitude / (squareD(scale) * 2.0D));
		this.radius = scale * SQRT_2;
	}

	public double getCenterX(long seed, int cellX, int cellY) {
		return (cellX + Permuter.toPositiveDouble(Permuter.permute(seed ^ 0x850750EC38D91CBCL, cellX, cellY))) * this.scale;
	}

	public double getCenterY(long seed, int cellX, int cellY) {
		return (cellY + Permuter.toPositiveDouble(Permuter.permute(seed ^ 0x567F93814C7E22A4L, cellX, cellY))) * this.scale;
	}

	@Override
	public double getValue(long seed, int x, int y) {
		seed ^= this.salt.value;
		int minCellX = Math.floorDiv( ceilI(x - this.radius), this.scale);
		int maxCellX = Math.floorDiv(floorI(x + this.radius), this.scale);
		int minCellY = Math.floorDiv( ceilI(y - this.radius), this.scale);
		int maxCellY = Math.floorDiv(floorI(y + this.radius), this.scale);
		double value = Double.POSITIVE_INFINITY;
		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
				double centerX = this.getCenterX(seed, cellX, cellY);
				double centerY = this.getCenterY(seed, cellX, cellY);
				value = Math.min(value, squareD(centerX - x, centerY - y));
			}
		}
		return value * this.rcp;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, double[] samples, int sampleCount) {
		seed ^= this.salt.value;
		Arrays.fill(samples, 0, sampleCount, Double.POSITIVE_INFINITY);
		int minCellX = Math.floorDiv( ceilI(startX - this.radius), this.scale);
		int maxCellX = Math.floorDiv(floorI(startX + sampleCount + this.radius), this.scale);
		int minCellY = Math.floorDiv( ceilI(y - this.radius), this.scale);
		int maxCellY = Math.floorDiv(floorI(y + this.radius), this.scale);
		double radius2 = squareD(this.radius);
		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
				double centerX = this.getCenterX(seed, cellX, cellY);
				double centerY = this.getCenterY(seed, cellX, cellY);
				double y2      = squareD(centerY - y);
				double offset  = Math.sqrt(radius2 - y2);
				if (offset > 0.0D) {
					int minX = Math.max( ceilI(centerX - offset), startX);
					int maxX = Math.min(floorI(centerX + offset), startX + sampleCount - 1);
					for (int x = minX; x <= maxX; x++) {
						int index = x - startX;
						samples[index] = Math.min(samples[index], squareD(centerX - x) + y2);
					}
				}
			}
		}
		for (int index = 0; index < sampleCount; index++) {
			samples[index] *= this.rcp;
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, double[] samples, int sampleCount) {
		seed ^= this.salt.value;
		Arrays.fill(samples, 0, sampleCount, Double.POSITIVE_INFINITY);
		int minCellX = Math.floorDiv( ceilI(x - this.radius), this.scale);
		int maxCellX = Math.floorDiv(floorI(x + this.radius), this.scale);
		int minCellY = Math.floorDiv( ceilI(startY - this.radius), this.scale);
		int maxCellY = Math.floorDiv(floorI(startY + sampleCount + this.radius), this.scale);
		double radius2 = squareD(this.radius);
		for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
			for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
				double centerX = this.getCenterX(seed, cellX, cellY);
				double centerY = this.getCenterY(seed, cellX, cellY);
				double x2      = squareD(centerX - x);
				double offset  = Math.sqrt(radius2 - x2);
				if (offset > 0.0D) {
					int minY = Math.max( ceilI(centerY - offset), startY);
					int maxY = Math.min(floorI(centerY + offset), startY + sampleCount - 1);
					for (int y = minY; y <= maxY; y++) {
						int index = y - startY;
						samples[index] = Math.min(samples[index], x2 + squareD(centerY - y));
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
		WorleyGrid2D grid = new WorleyGrid2D(new NumberSeed(Permuter.stafford(System.currentTimeMillis())), 10, 100.0D);
		grid.getBulkX(0L, 1000, 1000, values, values.length);
		for (double value : values) {
			System.out.println("#".repeat((int)(value)));
		}
	}
	*/
}