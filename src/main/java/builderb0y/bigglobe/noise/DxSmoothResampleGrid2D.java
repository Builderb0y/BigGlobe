package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.DerivativeSmoothPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmoothPolynomial;

public class DxSmoothResampleGrid2D extends Resample4Grid2D {

	public DxSmoothResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm2 polyFormX() {
		return DerivativeSmoothPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return SmoothPolynomial.FORM;
	}
}