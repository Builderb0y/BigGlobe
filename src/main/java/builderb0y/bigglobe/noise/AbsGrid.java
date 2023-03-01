package builderb0y.bigglobe.noise;

/**
a Grid implementation whose values will be the {@link Math#abs(double) absolute value}
of the values in another {@link #getGrid() wrapped grid}.
*/
public abstract class AbsGrid implements UnaryGrid {

	public final transient double minValue, maxValue;

	public AbsGrid(Grid grid) {
		double
			min = grid.minValue(),
			max = grid.maxValue(),
			absMin = Math.abs(min),
			absMax = Math.abs(max);
		if (min <= 0.0D && max >= 0.0D) {
			this.minValue = 0.0D;
		}
		else {
			this.minValue = Math.min(absMin, absMax);
		}
		this.maxValue = Math.max(absMin, absMax);
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
	public double operate(double value) {
		return Math.abs(value);
	}
}