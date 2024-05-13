package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;

public interface Polynomial {

	public abstract void push(double next);

	public abstract double interpolate(double fraction);

	public static abstract class Polynomial2 implements Polynomial {

		public double value0, value1;

		public Polynomial2(double value0, double value1) {
			this.update(this.value0 = value0, this.value1 = value1);
		}

		@Override
		public void push(double next) {
			this.update(this.value0 = this.value1, this.value1 = next);
		}

		public abstract void update(double value0, double value1);
	}

	public static class LinearPolynomial extends Polynomial2 {

		public double diff;

		public LinearPolynomial(double value0, double value1) {
			super(value0, value1);
		}

		@Override
		public void update(double value0, double value1) {
			this.diff = value1 - value0;
		}

		@Override
		public double interpolate(double fraction) {
			return this.diff * fraction + this.value0;
		}
	}

	public static class DerivativeLinearPolynomial extends Polynomial2 {

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
	}

	public static class SmoothPolynomial extends Polynomial2 {

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
	}

	public static class DerivativeSmoothPolynomial extends Polynomial2 {

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
	}

	public static class SmootherPolynomial extends Polynomial2 {

		public double term0, term1, term2;

		public SmootherPolynomial(double value0, double value1) {
			super(value0, value1);
		}

		@Override
		public void update(double value0, double value1) {
			double diff = value1 - value0;
			this.term0 = diff *   6.0D;
			this.term1 = diff * -15.0D;
			this.term2 = diff *  10.0D;
		}

		@Override
		public double interpolate(double fraction) {
			return ((fraction * this.term0 + this.term1) * fraction + this.term2) * fraction * fraction * fraction + this.value0;
		}
	}

	public static class DerivativeSmootherPolynomial extends Polynomial2 {

		public double scalar;

		public DerivativeSmootherPolynomial(double value0, double value1) {
			super(value0, value1);
		}

		@Override
		public void update(double value0, double value1) {
			this.scalar = (value1 - value0) * 30.0D;
		}

		@Override
		public double interpolate(double fraction) {
			return BigGlobeMath.squareD(fraction - fraction * fraction) * this.scalar;
		}
	}

	public static abstract class Polynomial4 implements Polynomial {

		public double value0, value1, value2, value3;

		public Polynomial4(double value0, double value1, double value2, double value3) {
			this.update(this.value0 = value0, this.value1 = value1, this.value2 = value2, this.value3 = value3);
		}

		@Override
		public void push(double next) {
			this.update(this.value0 = this.value1, this.value1 = this.value2, this.value2 = this.value3, this.value3 = next);
		}

		public abstract void update(double value0, double value1, double value2, double value3);
	}

	public static class CubicPolynomial extends Polynomial4 {

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
	}

	public static class DerivativeCubicPolynomial extends Polynomial4 {

		public double term0, term1, term2;

		public DerivativeCubicPolynomial(double value0, double value1, double value2, double value3) {
			super(value0, value1, value2, value3);
		}

		@Override
		public void update(double value0, double value1, double value2, double value3) {
			this.term0 = 4.5D * (value1 - value2) + 1.5D * (value3 - value0);
			this.term1 = 2.0D * value0 - (5.0D * value1) + (4.0D * value2) - value3;
			this.term2 = 0.5D * (value2 - value0);
		}

		@Override
		public double interpolate(double fraction) {
			return (fraction * this.term0 + this.term1) * fraction + this.term2;
		}
	}
}