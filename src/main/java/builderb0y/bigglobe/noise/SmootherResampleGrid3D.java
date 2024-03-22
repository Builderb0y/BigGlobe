package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;

public class SmootherResampleGrid3D extends Resample8Grid3D {

	public SmootherResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public double curveX(int fracX) {
		return Interpolator.smoother(fracX * this.rcpX);
	}

	@Override
	public double curveY(int fracY) {
		return Interpolator.smoother(fracY * this.rcpY);
	}

	@Override
	public double curveZ(int fracZ) {
		return Interpolator.smoother(fracZ * this.rcpZ);
	}
}