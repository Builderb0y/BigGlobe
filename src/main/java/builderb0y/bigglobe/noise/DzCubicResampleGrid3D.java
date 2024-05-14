package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.CubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.DerivativeCubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;

public class DzCubicResampleGrid3D extends Resample64Grid3D {

	public DzCubicResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm4 polyFormX() {
		return CubicPolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormY() {
		return CubicPolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormZ() {
		return DerivativeCubicPolynomial.FORM;
	}
}