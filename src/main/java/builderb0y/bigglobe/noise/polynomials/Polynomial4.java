package builderb0y.bigglobe.noise.polynomials;

public abstract class Polynomial4 implements Polynomial {

	public double value0, value1, value2, value3;

	public Polynomial4(double value0, double value1, double value2, double value3) {
		this.update(this.value0 = value0, this.value1 = value1, this.value2 = value2, this.value3 = value3);
	}

	@Override
	public void push(double next) {
		this.update(this.value0 = this.value1, this.value1 = this.value2, this.value2 = this.value3, this.value3 = next);
	}

	public abstract void update(double value0, double value1, double value2, double value3);

	public static interface PolyForm4 extends PolyForm {

		public abstract Polynomial createPolynomial(double value0, double value1, double value2, double value3);

		public abstract double interpolate(double value0, double value1, double value2, double value3, double fraction);
	}
}