package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;

public interface LayeredGrid3D extends LayeredGrid, Grid3D {

	@Override
	public abstract Grid3D[] getLayers();

	@Override
	public default double getValue(long seed, int x, int y, int z) {
		Grid3D[] layers = this.getLayers();
		double value = layers[0].getValue(seed, x, y, z);
		for (int index = 1, length = layers.length; index < length; index++) {
			value = this.accumulate(value, layers[index].getValue(seed, x, y, z));
		}
		return value;
	}

	@Override
	public default void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		if (samples.length() <= 0) return;
		Grid3D[] layers = this.getLayers();
		layers[0].getBulkX(seed, startX, y, z, samples);
		try (NumberArray scratch = NumberArray.allocateDoublesDirect(samples.length())) {
			for (int layerIndex = 1, length = layers.length; layerIndex < length; layerIndex++) {
				layers[layerIndex].getBulkX(seed, startX, y, z, scratch);
				this.accumulate(samples, scratch);
			}
		}
	}

	@Override
	public default void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		if (samples.length() <= 0) return;
		Grid3D[] layers = this.getLayers();
		layers[0].getBulkY(seed, x, startY, z, samples);
		try (NumberArray scratch = NumberArray.allocateDoublesDirect(samples.length())) {
			for (int layerIndex = 1, length = layers.length; layerIndex < length; layerIndex++) {
				layers[layerIndex].getBulkY(seed, x, startY, z, scratch);
				this.accumulate(samples, scratch);
			}
		}
	}

	@Override
	public default void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		if (samples.length() <= 0) return;
		Grid3D[] layers = this.getLayers();
		layers[0].getBulkZ(seed, x, y, startZ, samples);
		try (NumberArray scratch = NumberArray.allocateDoublesDirect(samples.length())) {
			for (int layerIndex = 1, length = layers.length; layerIndex < length; layerIndex++) {
				layers[layerIndex].getBulkZ(seed, x, y, startZ, scratch);
				this.accumulate(samples, scratch);
			}
		}
	}
}