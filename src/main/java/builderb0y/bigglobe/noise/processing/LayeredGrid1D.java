package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.NumberArray;

public interface LayeredGrid1D extends LayeredGrid, Grid1D {

	@Override
	public abstract Grid1D[] getLayers();

	@Override
	public default double getValue(long seed, int x) {
		Grid1D[] layers = this.getLayers();
		double value = layers[0].getValue(seed, x);
		for (int index = 1, length = layers.length; index < length; index++) {
			value = this.accumulate(value, layers[index].getValue(seed, x));
		}
		return value;
	}

	@Override
	public default void getBulkX(long seed, int startX, NumberArray samples) {
		if (samples.length() <= 0) return;
		Grid1D[] layers = this.getLayers();
		layers[0].getBulkX(seed, startX, samples);
		try (NumberArray scratch = NumberArray.allocateDoublesDirect(samples.length())) {
			for (int layerIndex = 1, length = layers.length; layerIndex < length; layerIndex++) {
				layers[layerIndex].getBulkX(seed, startX, scratch);
				this.accumulate(samples, scratch);
			}
		}
	}
}