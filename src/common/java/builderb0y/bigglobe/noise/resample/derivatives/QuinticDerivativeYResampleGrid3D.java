package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;
import builderb0y.bigglobe.noise.polynomials.QuinticDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.QuinticPolynomial;
import builderb0y.bigglobe.noise.resample.Resample64Grid3D;

public class QuinticDerivativeYResampleGrid3D extends Resample64Grid3D {

	public QuinticDerivativeYResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm4 polyFormX() {
		return QuinticPolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormY() {
		return QuinticDerivativePolynomial.FORM;
	}

	@Override
	public PolyForm4 polyFormZ() {
		return QuinticPolynomial.FORM;
	}
}