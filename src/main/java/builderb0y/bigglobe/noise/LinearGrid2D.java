package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

public class LinearGrid2D extends ValueGrid2D {

	public LinearGrid2D(Seed salt, double amplitude, int scaleX, int scaleY) {
		super(salt, amplitude, scaleX, scaleY);
	}

	@Override
	public double fracX(int fracX) {
		return fracX * this.rcpX;
	}

	@Override
	public double fracY(int fracY) {
		return fracY * this.rcpY;
	}
}