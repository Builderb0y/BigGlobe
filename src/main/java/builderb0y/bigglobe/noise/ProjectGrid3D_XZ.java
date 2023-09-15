package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.UseName;

public class ProjectGrid3D_XZ implements Grid3D {

	public final @UseName("2D_grid") Grid2D grid;

	public ProjectGrid3D_XZ(Grid2D grid) {
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
		return this.grid.getValue(seed, x, z);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		this.grid.getBulkX(seed, startX, z, samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		samples.fill(this.grid.getValue(seed, x, z));
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		this.grid.getBulkY(seed, x, startZ, samples);
	}
}