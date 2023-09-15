package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.UseName;

public class ProjectGrid3D_X implements Grid3D {

	public final @UseName("1D_grid") Grid1D grid;

	public ProjectGrid3D_X(Grid1D grid) {
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
		return this.grid.getValue(seed, x);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		this.grid.getBulkX(seed, startX, samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		samples.fill(this.grid.getValue(seed, x));
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		samples.fill(this.grid.getValue(seed, x));
	}
}