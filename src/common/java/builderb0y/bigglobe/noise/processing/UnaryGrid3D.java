package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;

public interface UnaryGrid3D extends UnaryGrid, Grid3D {

	@Override
	public abstract Grid3D getGrid();

	@Override
	public default double getValue(long seed, int x, int y, int z) {
		return this.operate(this.getGrid().getValue(seed, x, y, z));
	}

	@Override
	public default void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		this.getGrid().getBulkX(seed, startX, y, z, samples);
		this.operate(samples);
	}

	@Override
	public default void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		this.getGrid().getBulkY(seed, x, startY, z, samples);
		this.operate(samples);
	}

	@Override
	public default void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		this.getGrid().getBulkZ(seed, x, y, startZ, samples);
		this.operate(samples);
	}
}