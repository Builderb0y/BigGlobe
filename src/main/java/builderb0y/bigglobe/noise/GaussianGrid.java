package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.settings.Seed;

public abstract class GaussianGrid extends AbstractGrid {

	public final @VerifyIntRange(min = 2) int iterations;

	public GaussianGrid(Seed salt, double amplitude, int iterations) {
		super(salt, amplitude);
		this.iterations = iterations;
	}
}