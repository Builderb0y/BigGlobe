package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.CubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;

public class CubicResampleGrid1D extends Resample4Grid1D {

	public CubicResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm4 polyForm() {
		return CubicPolynomial.FORM;
	}
}