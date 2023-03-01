package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.noise.Permuter;

public record GaussianRandomSource(
	@UseName("min") double minValue,
	@UseName("max") @VerifySorted(greaterThan = "minValue") double maxValue,
	@VerifyIntRange(min = 0, minInclusive = false) int samples
)
implements RandomSource {

	@Override
	public double get(long seed) {
		double sum = 0.0D;
		for (int loop = this.samples; --loop >= 0;) {
			sum += Permuter.nextPositiveDouble(seed += Permuter.PHI64);
		}
		return this.mix(sum / this.samples);
	}

	@Override
	public double get(RandomGenerator random) {
		double sum = 0.0D;
		for (int loop = this.samples; --loop >= 0;) {
			sum += random.nextDouble();
		}
		return this.mix(sum / this.samples);
	}
}