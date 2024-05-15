package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.polynomials.CubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.CubicDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;
import builderb0y.bigglobe.noise.resample.Resample16Grid2D;

public class CubicDerivativeXResampleGrid2D extends Resample16Grid2D {

	public CubicDerivativeXResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm4 polyFormX() {
		return CubicDerivativePolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormY() {
		return CubicPolynomial.FORM;
	}
}