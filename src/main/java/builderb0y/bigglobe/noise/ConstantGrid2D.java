package builderb0y.bigglobe.noise;

public class ConstantGrid2D extends ConstantGrid implements Grid2D {

	public ConstantGrid2D(double value) {
		super(value);
	}

	@Override
	public double getValue(long seed, int x, int y) {
		return this.value;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		samples.fill(this.value);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		samples.fill(this.value);
	}
}