package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;

public class ExponentialRandomSource implements RandomSource {

	public final @VerifyFloatRange(min = 0.0D, minInclusive = false) double min;
	public final @VerifySorted(greaterThan = "min") double max;
	public final transient double logMin, logMax;

	public ExponentialRandomSource(double min, double max) {
		this.min = min;
		this.max = max;
		this.logMin = Math.log(min);
		this.logMax = Math.log(max);
	}

	public double curve(double unbiased) {
		return BigGlobeMath.exp(
			Interpolator.mixLinear(
				this.logMin,
				this.logMax,
				unbiased
			)
		);
	}

	@Override
	public double get(ScriptedColumn column, int y, long seed) {
		return this.curve(Permuter.nextPositiveDouble(seed));
	}

	@Override
	public double get(ScriptedColumn column, int y, RandomGenerator random) {
		return this.curve(random.nextDouble());
	}

	@Override
	public double minValue() {
		return this.min;
	}

	@Override
	public double maxValue() {
		return this.max;
	}
}