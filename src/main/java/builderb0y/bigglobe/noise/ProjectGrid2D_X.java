package builderb0y.bigglobe.noise;

import java.util.Arrays;

import builderb0y.autocodec.annotations.UseName;

public class ProjectGrid2D_X implements Grid2D {

	public final @UseName("1D_grid") Grid1D grid;

	public ProjectGrid2D_X(Grid1D grid) {
		this.grid = grid;
	}

	@Override
	public double minValue() {
		return this.grid.minValue();
	}

	@Override
	public double maxValue() {
		return this.grid.maxValue();
	}

	@Override
	public double getValue(long seed, int x, int y) {
		return this.grid.getValue(seed, x);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, double[] samples, int sampleCount) {
		this.grid.getBulkX(seed, startX, samples, sampleCount);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, double[] samples, int sampleCount) {
		Arrays.fill(samples, 0, sampleCount, this.grid.getValue(seed, x));
	}
}