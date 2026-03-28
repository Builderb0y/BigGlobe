package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.polynomials.CubicPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;

public class CubicResampleGrid2D extends Resample16Grid2D {

	public CubicResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm4 polyFormX() {
		return CubicPolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormY() {
		return CubicPolynomial.FORM;
	}
}