package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid;
import builderb0y.bigglobe.noise.NumberArray;

public interface LayeredGrid extends Grid {

	public abstract Grid[] getLayers();

	public abstract double accumulate(double a, double b);

	public default void accumulate(NumberArray mainSamples, NumberArray scratch) {
		for (int index = 0, length = mainSamples.length(); index < length; index++) {
			mainSamples.setD(index, this.accumulate(mainSamples.getD(index), scratch.getD(index)));
		}
	}
}