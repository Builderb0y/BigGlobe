package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.DerivativeLinearPolynomial;
import builderb0y.bigglobe.noise.polynomials.LinearPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;

public class DxLinearResampleGrid3D extends Resample8Grid3D {

	public DxLinearResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm2 polyFormX() {
		return DerivativeLinearPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return LinearPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormZ() {
		return LinearPolynomial.FORM;
	}
}