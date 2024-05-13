package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Polynomial.DerivativeSmootherPolynomial;
import builderb0y.bigglobe.noise.Polynomial.SmootherPolynomial;

public class DxSmootherResampleGrid2D extends Resample4Grid2D {

	public DxSmootherResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public double getMaxOvershoot() {
		return 1.875D;
	}

	@Override
	public Polynomial xPolynomial(double value0, double value1) {
		return new DerivativeSmootherPolynomial(value0, value1);
	}

	@Override
	public Polynomial yPolynomial(double value0, double value1) {
		return new SmootherPolynomial(value0, value1);
	}

	@Override
	public double interpolateX(double value0, double value1, double fraction) {
		return Interpolator.smootherDerivative(fraction) * (value1 - value0);
	}

	@Override
	public double interpolateY(double value0, double value1, double fraction) {
		return Interpolator.mixSmoother(value0, value1, fraction);
	}
}