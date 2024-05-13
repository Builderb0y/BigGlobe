package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Polynomial.DerivativeSmootherPolynomial;

public class DxSmootherResampleGrid1D extends Resample2Grid1D {

	public DxSmootherResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public double getMaxOvershoot() {
		return 1.875D;
	}

	@Override
	public Polynomial polynomial(double value0, double value1) {
		return new DerivativeSmootherPolynomial(value0, value1);
	}

	@Override
	public double interpolate(double value0, double value1, double fraction) {
		return BigGlobeMath.squareD(fraction - fraction * fraction) * 30.0D * (value1 - value0);
	}
}