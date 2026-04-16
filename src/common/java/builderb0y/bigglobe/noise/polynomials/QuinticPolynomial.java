package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class QuinticPolynomial extends Polynomial4 {

	public static final Form FORM = new Form();

	public double term0, term1, term2, term3, term4, term5;

	public QuinticPolynomial(double value0, double value1, double value2, double value3, double rcp) {
		super(value0, value1, value2, value3, rcp);
	}

	@Override
	public void update(double value0, double value1, double value2, double value3, double rcp) {
		this.term0 = Interpolator.quinticTerm0(value0, value1, value2, value3);
		this.term1 = Interpolator.quinticTerm1(value0, value1, value2, value3);
		this.term2 = Interpolator.quinticTerm2(value0, value1, value2, value3);
		this.term3 = Interpolator.quinticTerm3(value0, value1, value2, value3);
		this.term4 = Interpolator.quinticTerm4(value0, value1, value2, value3);
		this.term5 = Interpolator.quinticTerm5(value0, value1, value2, value3);
	}

	@Override
	public double interpolate(double fraction) {
		return Interpolator.combineQuinticTerms(this.term0, this.term1, this.term2, this.term3, this.term4, this.term5, fraction);
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form extends PolyForm4 {

		@Override
		public double calcMinValue(double min, double max, double rcp) {
			return Interpolator.mixLinear(max, min, OvershootConstants.QUINTIC);
		}

		@Override
		public double calcMaxValue(double min, double max, double rcp) {
			return Interpolator.mixLinear(min, max, OvershootConstants.QUINTIC);
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1, double value2, double value3, double rcp) {
			return new QuinticPolynomial(value0, value1, value2, value3, rcp);
		}

		@Override
		public double interpolate(double value0, double value1, double value2, double value3, double rcp, double fraction) {
			return Interpolator.mixQuintic(value0, value1, value2, value3, fraction);
		}
	}
}