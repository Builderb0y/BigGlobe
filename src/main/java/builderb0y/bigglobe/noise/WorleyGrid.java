package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.settings.Seed;

public abstract class WorleyGrid implements Grid {

	public final Seed salt;
	public final @VerifyIntRange(min = 0, minInclusive = false) int scale;
	public final double amplitude;
	public final transient double rcp;

	public WorleyGrid(Seed salt, int scale, double amplitude, double rcp) {
		this.salt = salt;
		this.scale = scale;
		this.amplitude = amplitude;
		this.rcp = rcp;
	}

	@Override
	public double minValue() {
		return Math.min(-this.amplitude, 0.0D);
	}

	@Override
	public double maxValue() {
		return Math.max(this.amplitude, 0.0D);
	}

	public void scale(NumberArray samples) {
		double rcp = this.rcp;
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.mul(index, rcp);
		}
	}
}