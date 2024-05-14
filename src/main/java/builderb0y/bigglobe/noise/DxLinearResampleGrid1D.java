package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.DerivativeLinearPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;

public class DxLinearResampleGrid1D extends Resample2Grid1D {

	public DxLinearResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm2 polyForm() {
		return DerivativeLinearPolynomial.FORM;
	}
}