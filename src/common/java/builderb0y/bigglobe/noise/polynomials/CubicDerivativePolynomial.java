package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class CubicDerivativePolynomial extends Polynomial4 {

	public static final Form FORM = new Form();

	public double term0, term1, term2;

	public CubicDerivativePolynomial(double value0, double value1, double value2, double value3, double rcp) {
		super(value0, value1, value2, value3, rcp);
	}

	@Override
	public void update(double value0, double value1, double value2, double value3, double rcp) {
		this.term0 = Interpolator.cubicDerivativeTerm0(value0, value1, value2, value3) * rcp;
		this.term1 = Interpolator.cubicDerivativeTerm1(value0, value1, value2, value3) * rcp;
		this.term2 = Interpolator.cubicDerivativeTerm2(value0, value1, value2, value3) * rcp;
	}

	@Override
	public double interpolate(double fraction) {
		return Interpolator.combineCubicDerivativeTerms(this.term0, this.term1, this.term2, fraction);
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form extends PolyForm4 {

		@Override
		public double calcMinValue(double min, double max, double rcp) {
			return (min - max) * rcp * OvershootConstants.DERIVATIVE_CUBIC;
		}

		@Override
		public double calcMaxValue(double min, double max, double rcp) {
			return (max - min) * rcp * OvershootConstants.DERIVATIVE_CUBIC;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1, double value2, double value3, double rcp) {
			return new CubicDerivativePolynomial(value0, value1, value2, value3, rcp);
		}

		@Override
		public double interpolate(double value0, double value1, double value2, double value3, double rcp, double fraction) {
			return Interpolator.cubicDerivative(value0, value1, value2, value3, fraction) * rcp;
		}
	}
}