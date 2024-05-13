package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.Polynomial.DerivativeSmoothPolynomial;

public class DxSmoothResampleGrid1D extends Resample2Grid1D {

	public DxSmoothResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public double getMaxOvershoot() {
		return 1.5D;
	}

	@Override
	public Polynomial polynomial(double value0, double value1) {
		return new DerivativeSmoothPolynomial(value0, value1);
	}

	@Override
	public double interpolate(double value0, double value1, double fraction) {
		return (6.0D - 6.0D * fraction) * fraction * (value1 - value0);
	}
}