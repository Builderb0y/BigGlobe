package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.polynomials.SmoothDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.resample.Resample2Grid1D;

public class SmoothDerivativeXResampleGrid1D extends Resample2Grid1D {

	public SmoothDerivativeXResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm2 polyForm() {
		return SmoothDerivativePolynomial.FORM;
	}
}