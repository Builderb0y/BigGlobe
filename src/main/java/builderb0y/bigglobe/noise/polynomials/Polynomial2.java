package builderb0y.bigglobe.noise.polynomials;

public abstract class Polynomial2 implements Polynomial {

	public double value0, value1;

	public Polynomial2(double value0, double value1) {
		this.update(this.value0 = value0, this.value1 = value1);
	}

	@Override
	public void push(double next) {
		this.update(this.value0 = this.value1, this.value1 = next);
	}

	public abstract void update(double value0, double value1);

	public static interface PolyForm2 extends PolyForm {

		public abstract Polynomial createPolynomial(double value0, double value1);

		public abstract double interpolate(double value0, double value1, double fraction);
	}
}