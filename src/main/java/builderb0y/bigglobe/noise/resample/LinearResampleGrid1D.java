package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.polynomials.LinearPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;

public class LinearResampleGrid1D extends Resample2Grid1D {

	public LinearResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm2 polyForm() {
		return LinearPolynomial.FORM;
	}
}