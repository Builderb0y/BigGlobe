package builderb0y.bigglobe.noise.polynomials;

public class LinearDerivativePolynomial extends Polynomial2 {

	public static final Form FORM = new Form();

	public double rcp, diff;

	public LinearDerivativePolynomial(double value0, double value1, double rcp) {
		super(value0, value1, rcp);
		this.rcp = rcp;
	}

	@Override
	public void update(double value0, double value1, double rcp) {
		this.diff = (value1 - value0) * rcp;
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
		public double calcMinValue(double min, double max, double rcp) {
			return (min - max) * rcp;
		}

		@Override
		public double calcMaxValue(double min, double max, double rcp) {
			return (max - min) * rcp;
		}

		@Override
		public Polynomial createPolynomial(double value0, double value1, double rcp) {
			return new LinearDerivativePolynomial(value0, value1, rcp);
		}

		@Override
		public double interpolate(double value0, double value1, double rcp, double fraction) {
			return (value1 - value0) * rcp;
		}
	}
}