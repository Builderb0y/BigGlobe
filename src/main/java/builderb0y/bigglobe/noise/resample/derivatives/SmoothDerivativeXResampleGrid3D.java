package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.polynomials.SmoothDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmoothPolynomial;
import builderb0y.bigglobe.noise.resample.Resample8Grid3D;

public class SmoothDerivativeXResampleGrid3D extends Resample8Grid3D {

	public SmoothDerivativeXResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm2 polyFormX() {
		return SmoothDerivativePolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return SmoothPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormZ() {
		return SmoothPolynomial.FORM;
	}
}