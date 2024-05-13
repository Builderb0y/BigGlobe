package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Polynomial.DerivativeCubicPolynomial;

public class DxCubicResampleGrid1D extends Resample4Grid1D {

	public DxCubicResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public double getMaxOvershoot() {
		return 1.5D;
	}

	@Override
	public Polynomial polynomial(double value0, double value1, double value2, double value3) {
		return new DerivativeCubicPolynomial(value0, value1, value2, value3);
	}

	@Override
	public double interpolate(double value0, double value1, double value2, double value3, double fraction) {
		return Interpolator.cubicDerivative(value0, value1, value2, value3, fraction);
	}
}