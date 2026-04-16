package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;
import builderb0y.bigglobe.noise.polynomials.QuinticPolynomial;

public class QuinticResampleGrid2D extends Resample16Grid2D {

	public QuinticResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public PolyForm4 polyFormX() {
		return QuinticPolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormY() {
		return QuinticPolynomial.FORM;
	}
}