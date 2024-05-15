package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.polynomials.CubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.CubicDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;
import builderb0y.bigglobe.noise.resample.Resample64Grid3D;

public class CubicDerivativeYResampleGrid3D extends Resample64Grid3D {

	public CubicDerivativeYResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm4 polyFormX() {
		return CubicPolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormY() {
		return CubicDerivativePolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormZ() {
		return CubicPolynomial.FORM;
	}
}