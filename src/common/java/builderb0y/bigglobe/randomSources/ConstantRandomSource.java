package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public record ConstantRandomSource(double value) implements RandomSource {

	@Override
	public double get(ScriptedColumn column, int y, long seed) {
		return this.value;
	}

	@Override
	public double get(ScriptedColumn column, int y, RandomGenerator random) {
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