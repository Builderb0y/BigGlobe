package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.DerivativeSmootherPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmootherPolynomial;

public class DxSmootherResampleGrid2D extends Resample4Grid2D {

	public DxSmootherResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm2 polyFormX() {
		return DerivativeSmootherPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return SmootherPolynomial.FORM;
	}
}