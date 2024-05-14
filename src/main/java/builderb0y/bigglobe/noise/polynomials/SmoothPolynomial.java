package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class SmoothPolynomial extends Polynomial2 {

	public static final Form FORM = new Form();

	public double term0, term1;

	public SmoothPolynomial(double value0, double value1) {
		super(value0, value1);
	}

	@Override
	public void update(double value0, double value1) {
		double diff = value1 - value0;
		this.term0 = diff * -2.0D;
		this.term1 = diff * +3.0D;
	}

	@Override
	public double interpolate(double fraction) {
		return (fraction * this.term0 + this.term1) * fraction * fraction + this.value0;
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form implements PolyForm2 {

		@Override
		public double getMaxOvershoot() {
			return OvershootConstants.SMOOTH;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1) {
			return new SmoothPolynomial(value0, value1);
		}

		@Override
		public double interpolate(double value0, double value1, double fraction) {
			return Interpolator.mixSmoothUnchecked(value0, value1, fraction);
		}
	}
}