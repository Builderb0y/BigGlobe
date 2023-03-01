package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

public class SmoothGrid1D extends ValueGrid1D {

	public SmoothGrid1D(Seed salt, double amplitude, int scaleX) {
		super(salt, amplitude, scaleX);
	}

	@Override
	public double fracX(int fracX) {
		return Interpolator.smooth(fracX * this.rcpX);
	}
}