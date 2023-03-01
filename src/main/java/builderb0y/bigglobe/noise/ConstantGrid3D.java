package builderb0y.bigglobe.noise;

public class ConstantGrid3D extends ConstantGrid implements Grid3D {

	public ConstantGrid3D(double value) {
		super(value);
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		return this.value;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, double[] samples, int sampleCount) {
		this.fill(samples, sampleCount);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, double[] samples, int sampleCount) {
		this.fill(samples, sampleCount);
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, double[] samples, int sampleCount) {
		this.fill(samples, sampleCount);
	}
}