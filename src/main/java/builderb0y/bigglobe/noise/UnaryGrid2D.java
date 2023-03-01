package builderb0y.bigglobe.noise;

public interface UnaryGrid2D extends UnaryGrid, Grid2D {

	@Override
	public abstract Grid2D getGrid();

	@Override
	public default double getValue(long seed, int x, int y) {
		return this.operate(this.getGrid().getValue(seed, x, y));
	}

	@Override
	public default void getBulkX(long seed, int startX, int y, double[] samples, int sampleCount) {
		this.getGrid().getBulkX(seed, startX, y, samples, sampleCount);
		this.operate(samples, sampleCount);
	}

	@Override
	public default void getBulkY(long seed, int x, int startY, double[] samples, int sampleCount) {
		this.getGrid().getBulkY(seed, x, startY, samples, sampleCount);
		this.operate(samples, sampleCount);
	}
}