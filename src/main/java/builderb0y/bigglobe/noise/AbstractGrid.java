package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.bigglobe.settings.Seed;

public abstract class AbstractGrid implements Grid {

	public final Seed salt;
	public final @VerifyFloatRange(min = 0.0D, minInclusive = false) double amplitude;

	public AbstractGrid(Seed salt, double amplitude) {
		this.salt = salt;
		this.amplitude = amplitude;
	}

	@Override
	public double minValue() {
		return -this.amplitude;
	}

	@Override
	public double maxValue() {
		return +this.amplitude;
	}

	public void scale(NumberArray samples) {
		double amplitude = this.amplitude;
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.mul(index, amplitude);
		}
	}
}