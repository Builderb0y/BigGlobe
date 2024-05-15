package builderb0y.bigglobe.noise.processing;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.NumberArray;

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
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		this.grid.getBulkX(seed, startX, samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		samples.fill(this.grid.getValue(seed, x));
	}
}