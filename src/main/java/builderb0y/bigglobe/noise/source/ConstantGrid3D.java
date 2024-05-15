package builderb0y.bigglobe.noise.source;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;

public class ConstantGrid3D extends ConstantGrid implements Grid3D {

	public ConstantGrid3D(double value) {
		super(value);
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		return this.value;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		samples.fill(this.value);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		samples.fill(this.value);
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		samples.fill(this.value);
	}
}