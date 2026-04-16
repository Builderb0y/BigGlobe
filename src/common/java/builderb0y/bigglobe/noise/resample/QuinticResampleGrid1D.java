package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;
import builderb0y.bigglobe.noise.polynomials.QuinticPolynomial;

public class QuinticResampleGrid1D extends Resample4Grid1D {

	public QuinticResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm4 polyForm() {
		return QuinticPolynomial.FORM;
	}
}