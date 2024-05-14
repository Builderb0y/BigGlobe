package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.DerivativeSmoothPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;

public class DxSmoothResampleGrid1D extends Resample2Grid1D {

	public DxSmoothResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm2 polyForm() {
		return DerivativeSmoothPolynomial.FORM;
	}
}