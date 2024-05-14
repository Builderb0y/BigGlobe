package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class CubicPolynomial extends Polynomial4 {

	public static final Form FORM = new Form();

	public double term0, term1, term2;

	public CubicPolynomial(double value0, double value1, double value2, double value3) {
		super(value0, value1, value2, value3);
	}

	@Override
	public void update(double value0, double value1, double value2, double value3) {
		this.term0 = 1.5D * (value1 - value2) + 0.5D * (value3 - value0);
		this.term1 = value0 - (2.5D * value1) + (2.0D * value2) - (0.5D * value3);
		this.term2 = 0.5D * (value2 - value0);
	}

	@Override
	public double interpolate(double fraction) {
		return ((fraction * this.term0 + this.term1) * fraction + this.term2) * fraction + this.value1;
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form implements PolyForm4 {

		@Override
		public double getMaxOvershoot() {
			return OvershootConstants.CUBIC;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1, double value2, double value3) {
			return new CubicPolynomial(value0, value1, value2, value3);
		}

		@Override
		public double interpolate(double value0, double value1, double value2, double value3, double fraction) {
			return Interpolator.mixCubic(value0, value1, value2, value3, fraction);
		}
	}
}