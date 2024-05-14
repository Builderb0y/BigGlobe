package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.CubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.DerivativeCubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;

public class DxCubicResampleGrid2D extends Resample16Grid2D {

	public DxCubicResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm4 polyFormX() {
		return DerivativeCubicPolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormY() {
		return CubicPolynomial.FORM;
	}
}