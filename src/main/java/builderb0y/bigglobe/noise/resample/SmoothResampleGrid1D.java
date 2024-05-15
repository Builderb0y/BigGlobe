package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmoothPolynomial;

public class SmoothResampleGrid1D extends Resample2Grid1D {

	public SmoothResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm2 polyForm() {
		return SmoothPolynomial.FORM;
	}
}