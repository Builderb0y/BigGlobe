package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid;

public abstract class ProductGrid implements LayeredGrid {

	public final transient double minValue, maxValue;

	public ProductGrid(Grid... layers) {
		double minValue = layers[0].minValue();
		double maxValue = layers[0].maxValue();
		for (int index = 1, length = layers.length; index < length; index++) {
			double
				newMin = layers[index].minValue(),
				newMax = layers[index].maxValue(),
				product1 = minValue * newMin,
				product2 = minValue * newMax,
				product3 = maxValue * newMin,
				product4 = maxValue * newMax;
			minValue = Math.min(Math.min(product1, product2), Math.min(product3, product4));
			maxValue = Math.max(Math.max(product1, product2), Math.max(product3, product4));
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
		return true;
	}
}