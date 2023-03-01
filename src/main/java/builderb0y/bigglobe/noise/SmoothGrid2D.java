package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

public class SmoothGrid2D extends ValueGrid2D {

	public SmoothGrid2D(Seed salt, double amplitude, int scaleX, int scaleY) {
		super(salt, amplitude, scaleX, scaleY);
	}

	@Override
	public double fracX(int fracX) {
		return Interpolator.smooth(fracX * this.rcpX);
	}

	@Override
	public double fracY(int fracY) {
		return Interpolator.smooth(fracY * this.rcpY);
	}
}