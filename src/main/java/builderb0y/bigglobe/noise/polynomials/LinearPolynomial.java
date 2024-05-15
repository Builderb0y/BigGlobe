package builderb0y.bigglobe.noise.polynomials;

import builderb0y.bigglobe.math.Interpolator;

public class LinearPolynomial extends Polynomial2 {

	public static final Form FORM = new Form();

	public double diff;

	public LinearPolynomial(double value0, double value1, double rcp) {
		super(value0, value1, rcp);
	}

	@Override
	public void update(double value0, double value1, double rcp) {
		this.diff = value1 - value0;
	}

	@Override
	public double interpolate(double fraction) {
		return this.diff * fraction + this.value0;
	}

	@Override
	public PolyForm form() {
		return FORM;
	}

	public static class Form implements PolyForm2 {

		@Override
		public double calcMinValue(double min, double max, double rcp) {
			return min;
		}

		@Override
		public double calcMaxValue(double min, double max, double rcp) {
			return max;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1, double rcp) {
			return new LinearPolynomial(value0, value1, rcp);
		}

		@Override
		public double interpolate(double value0, double value1, double rcp, double fraction) {
			return Interpolator.mixLinear(value0, value1, fraction);
		}
	}
}