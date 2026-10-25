package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.noise.Permuter;

public record UniformRandomSource(
	@UseName("min") double minValue,
	@UseName("max") @VerifySorted(greaterThan = "minValue") double maxValue
)
	implements RandomSource {

	@Override
	public double get(ScriptedColumn column, int y, long seed) {
		return this.mix(Permuter.nextPositiveDouble(seed));
	}

	@Override
	public double get(ScriptedColumn column, int y, RandomGenerator random) {
		return this.mix(random.nextDouble());
	}
}