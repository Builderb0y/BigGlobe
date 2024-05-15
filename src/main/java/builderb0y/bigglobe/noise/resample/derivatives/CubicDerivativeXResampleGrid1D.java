package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.polynomials.CubicDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;
import builderb0y.bigglobe.noise.resample.Resample4Grid1D;

public class CubicDerivativeXResampleGrid1D extends Resample4Grid1D {

	public CubicDerivativeXResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public PolyForm4 polyForm() {
		return CubicDerivativePolynomial.FORM;
	}
}