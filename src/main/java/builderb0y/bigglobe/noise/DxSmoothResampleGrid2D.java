package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Polynomial.DerivativeSmoothPolynomial;
import builderb0y.bigglobe.noise.Polynomial.SmoothPolynomial;

public class DxSmoothResampleGrid2D extends Resample4Grid2D {

	public DxSmoothResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public double getMaxOvershoot() {
		return 1.5D;
	}

	@Override
	public Polynomial xPolynomial(double value0, double value1) {
		return new DerivativeSmoothPolynomial(value0, value1);
	}

	@Override
	public Polynomial yPolynomial(double value0, double value1) {
		return new SmoothPolynomial(value0, value1);
	}

	@Override
	public double interpolateX(double value0, double value1, double fraction) {
		return Interpolator.smoothDerivative(fraction) * (value1 - value0);
	}

	@Override
	public double interpolateY(double value0, double value1, double fraction) {
		return Interpolator.mixSmooth(value0, value1, fraction);
	}
}