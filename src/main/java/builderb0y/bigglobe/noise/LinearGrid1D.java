package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

public class LinearGrid1D extends ValueGrid1D {

	public LinearGrid1D(Seed salt, double amplitude, int scaleX) {
		super(salt, amplitude, scaleX);
	}

	@Override
	public double fracX(int fracX) {
		return fracX * this.rcpX;
	}
}