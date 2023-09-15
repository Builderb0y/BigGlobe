package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.UseName;

public class ProjectGrid2D_Y implements Grid2D {

	public final @UseName("1D_grid") Grid1D grid;

	public ProjectGrid2D_Y(Grid1D grid) {
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
		return this.grid.getValue(seed, y);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		samples.fill(this.grid.getValue(seed, y));
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		this.grid.getBulkX(seed, startY, samples);
	}
}