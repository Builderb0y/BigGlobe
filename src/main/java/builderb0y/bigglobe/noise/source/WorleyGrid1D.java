package builderb0y.bigglobe.noise.source;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class WorleyGrid1D extends WorleyGrid implements Grid1D {

	public WorleyGrid1D(Seed salt, int scale, double amplitude) {
		super(salt, scale, amplitude, amplitude / squareD(scale));
	}

	public double getCenterX(long seed, int cellX) {
		return (cellX + Permuter.toPositiveDouble(Permuter.permute(seed ^ 0xB8431F059B6D6735L, cellX))) * this.scale;
	}

	@Override
	public double getValue(long seed, int x) {
		seed ^= this.salt.value;
		int minCellX = Math.floorDiv(x - this.scale, this.scale);
		int maxCellX = Math.floorDiv(x + this.scale, this.scale);
		double value = Double.POSITIVE_INFINITY;
		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			double center = this.getCenterX(seed, cellX);
			value = Math.min(value, squareD(center - x));
		}
		return value * this.rcp;
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		int sampleCount = samples.length();
		seed ^= this.salt.value;
		samples.fill(Double.POSITIVE_INFINITY);
		int minCellX = Math.floorDiv(startX - this.scale, this.scale);
		int maxCellX = Math.floorDiv(startX + sampleCount + this.scale, this.scale);
		for (int cell = minCellX; cell <= maxCellX; cell++) {
			double center = this.getCenterX(seed, cell);
			int minX = Math.max( ceilI(center - this.scale), startX);
			int maxX = Math.min(floorI(center + this.scale), startX + sampleCount - 1);
			for (int x = minX; x <= maxX; x++) {
				int index = x - startX;
				samples.min(index, squareD(center - x));
			}
		}
		this.scale(samples);
	}

	/*
	public static void main(String[] args) {
		double[] values = new double[100];
		WorleyGrid1D grid = new WorleyGrid1D(new NumberSeed(Permuter.stafford(System.currentTimeMillis())), 10, 100.0D);
		grid.getBulkX(0L, 1000, values, values.length);
		for (double value : values) {
			System.out.println("#".repeat((int)(value)));
		}
	}
	*/
}