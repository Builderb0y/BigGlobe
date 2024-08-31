package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid;

/** a Grid implementation whose values are the sum of values in other Grid's. */
public abstract class SummingGrid implements LayeredGrid {

	public final transient double minValue, maxValue;

	public SummingGrid(Grid... layers) {
		double minValue = layers[0].minValue();
		double maxValue = layers[0].maxValue();
		for (int index = 1, length = layers.length; index < length; index++) {
			minValue += layers[index].minValue();
			maxValue += layers[index].maxValue();
		}
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public double minValue() {
		return this.minValue;
	}

	@Override
	public double maxValue() {
		return this.maxValue;
	}

	@Override
	public boolean isProduct() {
		return false;
	}
}