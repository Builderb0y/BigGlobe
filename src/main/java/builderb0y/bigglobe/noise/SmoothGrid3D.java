package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

public class SmoothGrid3D extends ValueGrid3D {

	public SmoothGrid3D(Seed salt, double amplitude, int scaleX, int scaleY, int scaleZ) {
		super(salt, amplitude, scaleX, scaleY, scaleZ);
	}

	@Override
	public double fracX(int fracX) {
		return Interpolator.smooth(fracX * this.rcpX);
	}

	@Override
	public double fracY(int fracY) {
		return Interpolator.smooth(fracY * this.rcpY);
	}

	@Override
	public double fracZ(int fracZ) {
		return Interpolator.smooth(fracZ * this.rcpZ);
	}
}