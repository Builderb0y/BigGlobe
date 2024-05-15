package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class SmoothDerivativePolynomial extends Polynomial2 {

	public static final Form FORM = new Form();

	public double rcp, scalar;

	public SmoothDerivativePolynomial(double value0, double value1, double rcp) {
		super(value0, value1, rcp);
		this.rcp = rcp;
	}

	@Override
	public void update(double value0, double value1, double rcp) {
		this.scalar = (value1 - value0) * 6.0D * rcp;
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
		public double calcMinValue(double min, double max, double rcp) {
			return (min - max) * rcp * OvershootConstants.DERIVATIVE_SMOOTH;
		}

		@Override
		public double calcMaxValue(double min, double max, double rcp) {
			return (max - min) * rcp * OvershootConstants.DERIVATIVE_SMOOTH;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1, double rcp) {
			return new SmoothDerivativePolynomial(value0, value1, rcp);
		}

		@Override
		public double interpolate(double value0, double value1, double rcp, double fraction) {
			return Interpolator.smoothDerivative(fraction) * (value1 - value0) * rcp;
		}
	}
}