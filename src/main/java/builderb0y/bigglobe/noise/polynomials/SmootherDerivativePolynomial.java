package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;

public class SmootherDerivativePolynomial extends Polynomial2 {

	public static final Form FORM = new Form();

	public double rcp, scalar;

	public SmootherDerivativePolynomial(double value0, double value1, double rcp) {
		super(value0, value1, rcp);
		this.rcp = rcp;
	}

	@Override
	public void update(double value0, double value1, double rcp) {
		this.scalar = (value1 - value0) * 30.0D * rcp;
	}

	@Override
	public double interpolate(double fraction) {
		return BigGlobeMath.squareD(fraction - fraction * fraction) * this.scalar;
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form implements PolyForm2 {

		@Override
		public double calcMinValue(double min, double max, double rcp) {
			return (min - max) * rcp * OvershootConstants.DERIVATIVE_SMOOTHER;
		}

		@Override
		public double calcMaxValue(double min, double max, double rcp) {
			return (max - min) * rcp * OvershootConstants.DERIVATIVE_SMOOTHER;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1, double rcp) {
			return new SmootherDerivativePolynomial(value0, value1, rcp);
		}

		@Override
		public double interpolate(double value0, double value1, double rcp, double fraction) {
			return Interpolator.smootherDerivative(fraction) * (value1 - value0) * rcp;
		}
	}
}