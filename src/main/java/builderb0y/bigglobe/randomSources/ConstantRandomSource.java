package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;

public record ConstantRandomSource(double value) implements RandomSource {

	@Override
	public double get(long seed) {
		return this.value;
	}

	@Override
	public double get(RandomGenerator random) {
		return this.value;
	}

	@Override
	public double minValue() {
		return this.value;
	}

	@Override
	public double maxValue() {
		return this.value;
	}
}