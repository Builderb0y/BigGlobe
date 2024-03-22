package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;

public class SmoothResampleGrid3D extends Resample8Grid3D {

	public SmoothResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public double curveX(int fracX) {
		return Interpolator.smooth(fracX * this.rcpX);
	}

	@Override
	public double curveY(int fracY) {
		return Interpolator.smooth(fracY * this.rcpY);
	}

	@Override
	public double curveZ(int fracZ) {
		return Interpolator.smooth(fracZ * this.rcpZ);
	}
}