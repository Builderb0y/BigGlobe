package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.polynomials.LinearDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.LinearPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.resample.Resample4Grid2D;

public class LinearDerivativeXResampleGrid2D extends Resample4Grid2D {

	public LinearDerivativeXResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm2 polyFormX() {
		return LinearDerivativePolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return LinearPolynomial.FORM;
	}
}