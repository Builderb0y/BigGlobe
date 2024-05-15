package builderb0y.bigglobe.noise.source;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.NumberArray;

public class ConstantGrid1D extends ConstantGrid implements Grid1D {

	public ConstantGrid1D(double value) {
		super(value);
	}

	@Override
	public double getValue(long seed, int x) {
		return this.value;
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		samples.fill(this.value);
	}
}