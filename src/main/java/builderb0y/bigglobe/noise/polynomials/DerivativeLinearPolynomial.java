package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class DerivativeLinearPolynomial extends Polynomial2 {

	public static final Form FORM = new Form();

	public double diff;

	public DerivativeLinearPolynomial(double value0, double value1) {
		super(value0, value1);
	}

	@Override
	public void update(double value0, double value1) {
		this.diff = value1 - value0;
	}

	@Override
	public double interpolate(double fraction) {
		return this.diff;
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form implements PolyForm2 {

		@Override
		public Polynomial createPolynomial(double value0, double value1) {
			return new DerivativeLinearPolynomial(value0, value1);
		}

		@Override
		public double interpolate(double value0, double value1, double fraction) {
			return Interpolator.mixLinear(value0, value1, fraction);
		}

		@Override
		public double getMaxOvershoot() {
			return OvershootConstants.DERIVATIVE_LINEAR;
		}
	}
}