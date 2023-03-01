package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.noise.Permuter;

/** special case of {@link GaussianRandomSource} where samples = 2. */
public record LinearCenteredRandomSource(
	@UseName("min") double minValue,
	@UseName("max") @VerifySorted(greaterThan = "minValue") double maxValue
)
implements RandomSource {

	@Override
	public double get(long seed) {
		return this.mix(
			(
				+ Permuter.nextPositiveDouble(seed += Permuter.PHI64)
				+ Permuter.nextPositiveDouble(seed += Permuter.PHI64)
			)
			* 0.5D
		);
	}

	@Override
	public double get(RandomGenerator random) {
		return this.mix((random.nextDouble() + random.nextDouble()) * 0.5D);
	}
}