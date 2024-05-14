package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.DerivativeCubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;

public class DxCubicResampleGrid1D extends Resample4Grid1D {

	public DxCubicResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm4 polyForm() {
		return DerivativeCubicPolynomial.FORM;
	}
}