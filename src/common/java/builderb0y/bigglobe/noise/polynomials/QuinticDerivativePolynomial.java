package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class QuinticDerivativePolynomial extends Polynomial4 {

	public static final Form FORM = new Form();

	public double term0, term1, term2, term3, term4;

	public QuinticDerivativePolynomial(double value0, double value1, double value2, double value3, double rcp) {
		super(value0, value1, value2, value3, rcp);
	}

	@Override
	public void update(double value0, double value1, double value2, double value3, double rcp) {
		this.term0 = Interpolator.quinticDerivativeTerm0(value0, value1, value2, value3) * rcp;
		this.term1 = Interpolator.quinticDerivativeTerm1(value0, value1, value2, value3) * rcp;
		this.term2 = Interpolator.quinticDerivativeTerm2(value0, value1, value2, value3) * rcp;
		this.term3 = Interpolator.quinticDerivativeTerm3(value0, value1, value2, value3) * rcp;
		this.term4 = Interpolator.quinticDerivativeTerm4(value0, value1, value2, value3) * rcp;
	}

	@Override
	public double interpolate(double fraction) {
		return Interpolator.combineQuinticDerivativeTerms(this.term0, this.term1, this.term2, this.term3, this.term4, fraction);
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form extends PolyForm4 {

		@Override
		public double calcMinValue(double min, double max, double rcp) {
			return (min - max) * rcp * OvershootConstants.DERIVATIVE_QUINTIC;
		}

		@Override
		public double calcMaxValue(double min, double max, double rcp) {
			return (min - max) * rcp * OvershootConstants.DERIVATIVE_QUINTIC;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1, double value2, double value3, double rcp) {
			return new QuinticDerivativePolynomial(value0, value1, value2, value3, rcp);
		}

		@Override
		public double interpolate(double value0, double value1, double value2, double value3, double rcp, double fraction) {
			return Interpolator.quinticDerivative(value0, value1, value2, value3, fraction) * rcp;
		}
	}
}