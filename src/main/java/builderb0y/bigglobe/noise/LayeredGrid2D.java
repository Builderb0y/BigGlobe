package builderb0y.bigglobe.noise;

public interface LayeredGrid2D extends LayeredGrid, Grid2D {

	@Override
	public abstract Grid2D[] getLayers();

	@Override
	public default double getValue(long seed, int x, int y) {
		Grid2D[] layers = this.getLayers();
		double value = layers[0].getValue(seed, x, y);
		for (int index = 1, length = layers.length; index < length; index++) {
			value = this.accumulate(value, layers[index].getValue(seed, x, y));
		}
		return value;
	}

	@Override
	public default void getBulkX(long seed, int startX, int y, NumberArray samples) {
		if (samples.length() <= 0) return;
		Grid2D[] layers = this.getLayers();
		layers[0].getBulkX(seed, startX, y, samples);
		try (NumberArray scratch = NumberArray.allocateDoublesDirect(samples.length())) {
			for (int layerIndex = 1, length = layers.length; layerIndex < length; layerIndex++) {
				layers[layerIndex].getBulkX(seed, startX, y, scratch);
				this.accumulate(samples, scratch);
			}
		}
	}

	@Override
	public default void getBulkY(long seed, int x, int startY, NumberArray samples) {
		if (samples.length() <= 0) return;
		Grid2D[] layers = this.getLayers();
		layers[0].getBulkY(seed, x, startY, samples);
		try (NumberArray scratch = NumberArray.allocateDoublesDirect(samples.length())) {
			for (int layerIndex = 1, length = layers.length; layerIndex < length; layerIndex++) {
				layers[layerIndex].getBulkY(seed, x, startY, scratch);
				this.accumulate(samples, scratch);
			}
		}
	}
}