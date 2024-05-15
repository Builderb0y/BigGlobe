package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.polynomials.SmoothDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmoothPolynomial;
import builderb0y.bigglobe.noise.resample.Resample4Grid2D;

public class SmoothDerivativeYResampleGrid2D extends Resample4Grid2D {

	public SmoothDerivativeYResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm2 polyFormX() {
		return SmoothPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return SmoothDerivativePolynomial.FORM;
	}
}