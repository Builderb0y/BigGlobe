package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.polynomials.SmootherDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmootherPolynomial;
import builderb0y.bigglobe.noise.resample.Resample4Grid2D;

public class SmootherDerivativeYResampleGrid2D extends Resample4Grid2D {

	public SmootherDerivativeYResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm2 polyFormX() {
		return SmootherPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return SmootherDerivativePolynomial.FORM;
	}
}