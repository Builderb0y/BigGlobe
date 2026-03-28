package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid;
import builderb0y.bigglobe.noise.NumberArray;

public interface UnaryGrid extends Grid {

	public abstract Grid getGrid();

	public abstract double operate(double value);

	public default void operate(NumberArray samples) {
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.setD(index, this.operate(samples.getD(index)));
		}
	}
}