package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.Polynomial.DerivativeLinearPolynomial;

public class DxLinearResampleGrid1D extends Resample2Grid1D {

	public DxLinearResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public double getMaxOvershoot() {
		return 1.0D;
	}

	@Override
	public Polynomial polynomial(double value0, double value1) {
		return new DerivativeLinearPolynomial(value0, value1);
	}

	@Override
	public double interpolate(double value0, double value1, double fraction) {
		return value1 - value0;
	}
}