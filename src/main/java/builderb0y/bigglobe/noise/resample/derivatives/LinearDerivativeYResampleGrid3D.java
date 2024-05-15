package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.polynomials.LinearDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.LinearPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.resample.Resample8Grid3D;

public class LinearDerivativeYResampleGrid3D extends Resample8Grid3D {

	public LinearDerivativeYResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm2 polyFormX() {
		return LinearPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return LinearDerivativePolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormZ() {
		return LinearPolynomial.FORM;
	}
}