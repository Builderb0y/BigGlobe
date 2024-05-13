package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Polynomial.CubicPolynomial;
import builderb0y.bigglobe.noise.Polynomial.DerivativeCubicPolynomial;

public class DxCubicResampleGrid2D extends Resample16Grid2D {

	public DxCubicResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public double getMaxOvershoot() {
		return 1.5D;
	}

	@Override
	public Polynomial xPolynomial(double value0, double value1, double value2, double value3) {
		return new DerivativeCubicPolynomial(value0, value1, value2, value3);
	}

	@Override
	public Polynomial yPolynomial(double value0, double value1, double value2, double value3) {
		return new CubicPolynomial(value0, value1, value2, value3);
	}

	@Override
	public double interpolateX(double value0, double value1, double value2, double value3, double fraction) {
		return Interpolator.cubicDerivative(value0, value1, value2, value3, fraction);
	}

	@Override
	public double interpolateY(double value0, double value1, double value2, double value3, double fraction) {
		return Interpolator.mixCubic(value0, value1, value2, value3, fraction);
	}
}