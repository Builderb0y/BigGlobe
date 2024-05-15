package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.NumberArray;

public interface UnaryGrid2D extends UnaryGrid, Grid2D {

	@Override
	public abstract Grid2D getGrid();

	@Override
	public default double getValue(long seed, int x, int y) {
		return this.operate(this.getGrid().getValue(seed, x, y));
	}

	@Override
	public default void getBulkX(long seed, int startX, int y, NumberArray samples) {
		this.getGrid().getBulkX(seed, startX, y, samples);
		this.operate(samples);
	}

	@Override
	public default void getBulkY(long seed, int x, int startY, NumberArray samples) {
		this.getGrid().getBulkY(seed, x, startY, samples);
		this.operate(samples);
	}
}