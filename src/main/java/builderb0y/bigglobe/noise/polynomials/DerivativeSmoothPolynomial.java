package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class DerivativeSmoothPolynomial extends Polynomial2 {

	public static final Form FORM = new Form();

	public double scalar;

	public DerivativeSmoothPolynomial(double value0, double value1) {
		super(value0, value1);
	}

	@Override
	public void update(double value0, double value1) {
		this.scalar = (value1 - value0) * 6.0D;
	}

	@Override
	public double interpolate(double fraction) {
		return (fraction - fraction * fraction) * this.scalar;
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form implements PolyForm2 {

		@Override
		public double getMaxOvershoot() {
			return OvershootConstants.DERIVATIVE_SMOOTH;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1) {
			return new DerivativeSmoothPolynomial(value0, value1);
		}

		@Override
		public double interpolate(double value0, double value1, double fraction) {
			return Interpolator.smoothDerivative(fraction) * (value1 - value0);
		}
	}
}