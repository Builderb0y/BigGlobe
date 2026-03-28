package builderb0y.bigglobe.noise.processing;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;

public class StackedGrid_XY implements Grid3D {

	public final @UseName("2D_grid") Grid2D grid;

	public StackedGrid_XY(@UseName("2D_grid") Grid2D grid) {
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
	public double getValue(long seed, int x, int y, int z) {
		return this.grid.getValue(Permuter.permute(seed, z), x, y);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		this.grid.getBulkX(Permuter.permute(seed, z), startX, y, samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		this.grid.getBulkY(Permuter.permute(seed, z), x, startY, samples);
	}
}