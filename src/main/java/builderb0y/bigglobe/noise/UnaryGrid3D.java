package builderb0y.bigglobe.noise;

public interface UnaryGrid3D extends UnaryGrid, Grid3D {

	@Override
	public abstract Grid3D getGrid();

	@Override
	public default double getValue(long seed, int x, int y, int z) {
		return this.operate(this.getGrid().getValue(seed, x, y, z));
	}

	@Override
	public default void getBulkX(long seed, int startX, int y, int z, double[] samples, int sampleCount) {
		this.getGrid().getBulkX(seed, startX, y, z, samples, sampleCount);
		this.operate(samples, sampleCount);
	}

	@Override
	public default void getBulkY(long seed, int x, int startY, int z, double[] samples, int sampleCount) {
		this.getGrid().getBulkY(seed, x, startY, z, samples, sampleCount);
		this.operate(samples, sampleCount);
	}

	@Override
	public default void getBulkZ(long seed, int x, int y, int startZ, double[] samples, int sampleCount) {
		this.getGrid().getBulkZ(seed, x, y, startZ, samples, sampleCount);
		this.operate(samples, sampleCount);
	}
}