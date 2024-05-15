package builderb0y.bigglobe.noise.processing;

public abstract class NegateGrid implements UnaryGrid {

	@Override
	public double minValue() {
		return -this.getGrid().maxValue();
	}

	@Override
	public double maxValue() {
		return -this.getGrid().minValue();
	}

	@Override
	public double operate(double value) {
		return -value;
	}
}