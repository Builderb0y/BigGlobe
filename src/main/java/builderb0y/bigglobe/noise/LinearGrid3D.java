package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

public class LinearGrid3D extends ValueGrid3D {

	public LinearGrid3D(Seed salt, double amplitude, int scaleX, int scaleY, int scaleZ) {
		super(salt, amplitude, scaleX, scaleY, scaleZ);
	}

	@Override
	public double fracX(int fracX) {
		return fracX * this.rcpX;
	}

	@Override
	public double fracY(int fracY) {
		return fracY * this.rcpY;
	}

	@Override
	public double fracZ(int fracZ) {
		return fracZ * this.rcpZ;
	}
}