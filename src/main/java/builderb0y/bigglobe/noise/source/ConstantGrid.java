package builderb0y.bigglobe.noise.source;

import builderb0y.bigglobe.noise.Grid;

/** a Grid implementation with a single constant value at all locations. */
public abstract class ConstantGrid implements Grid {

	public final double value;

	public ConstantGrid(double value) {
		this.value = value;
	}

	@Override
	public double minValue() {
		return this.value;
	}

	@Override
	public double maxValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + '(' + this.value + ')';
	}
}