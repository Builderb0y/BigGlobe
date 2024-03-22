package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;

public class SmootherResampleGrid2D extends Resample4Grid2D {

	public SmootherResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public double curveX(int fracX) {
		return Interpolator.smoother(fracX * this.rcpX);
	}

	@Override
	public double curveY(int fracY) {
		return Interpolator.smoother(fracY * this.rcpY);
	}
}