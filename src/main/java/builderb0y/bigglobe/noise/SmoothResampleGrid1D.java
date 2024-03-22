package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;

public class SmoothResampleGrid1D extends Resample2Grid1D {

	public SmoothResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public double curveX(int fracX) {
		return Interpolator.smooth(fracX * this.rcpX);
	}
}