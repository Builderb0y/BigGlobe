package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid;

/**
a Grid implementation whose values will be the {@link BigGlobeMath#squareD(double) square}
of the values in another {@link #getGrid() wrapped grid}.
*/
public abstract class SquareGrid implements UnaryGrid {

	public final transient double minValue, maxValue;

	public SquareGrid(Grid grid) {
		double
			min = grid.minValue(),
			max = grid.maxValue(),
			squareMin = BigGlobeMath.squareD(min),
			squareMax = BigGlobeMath.squareD(max);
		if (min <= 0.0D && max >= 0.0D) {
			this.minValue = 0.0D;
		}
		else {
			this.minValue = Math.min(squareMin, squareMax);
		}
		this.maxValue = Math.max(squareMin, squareMax);
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
		return value * value;
	}
}