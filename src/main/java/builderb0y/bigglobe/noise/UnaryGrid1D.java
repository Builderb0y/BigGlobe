package builderb0y.bigglobe.noise;

public interface UnaryGrid1D extends UnaryGrid, Grid1D {

	@Override
	public abstract Grid1D getGrid();

	@Override
	public default double getValue(long seed, int x) {
		return this.operate(this.getGrid().getValue(seed, x));
	}

	@Override
	public default void getBulkX(long seed, int startX, double[] samples, int sampleCount) {
		this.getGrid().getBulkX(seed, startX, samples, sampleCount);
		this.operate(samples, sampleCount);
	}
}