package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;
import builderb0y.bigglobe.noise.polynomials.QuinticDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.QuinticPolynomial;
import builderb0y.bigglobe.noise.resample.Resample16Grid2D;

public class QuinticDerivativeXResampleGrid2D extends Resample16Grid2D {

	public QuinticDerivativeXResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm4 polyFormX() {
		return QuinticDerivativePolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormY() {
		return QuinticPolynomial.FORM;
	}
}