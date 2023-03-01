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
	default void getBulkX(long seed, int startX, int y, double[] samples, int sampleCount) {
		if (sampleCount <= 0) return;
		Grid2D[] layers = this.getLayers();
		layers[0].getBulkX(seed, startX, y, samples, sampleCount);
		double[] scratch = Grid.getScratchArray(sampleCount);
		try {
			for (int layerIndex = 1, length = layers.length; layerIndex < length; layerIndex++) {
				layers[layerIndex].getBulkX(seed, startX, y, scratch, sampleCount);
				this.accumulate(samples, scratch, sampleCount);
			}
		}
		finally {
			Grid.reclaimScratchArray(scratch);
		}
	}

	@Override
	default void getBulkY(long seed, int x, int startY, double[] samples, int sampleCount) {
		if (sampleCount <= 0) return;
		Grid2D[] layers = this.getLayers();
		layers[0].getBulkY(seed, x, startY, samples, sampleCount);
		double[] scratch = Grid.getScratchArray(sampleCount);
		try {
			for (int layerIndex = 1, length = layers.length; layerIndex < length; layerIndex++) {
				layers[layerIndex].getBulkY(seed, x, startY, scratch, sampleCount);
				this.accumulate(samples, scratch, sampleCount);
			}
		}
		finally {
			Grid.reclaimScratchArray(scratch);
		}
	}
}