package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.DerivativeSmootherPolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmootherPolynomial;

public class DzSmootherResampleGrid3D extends Resample8Grid3D {

	public DzSmootherResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm2 polyFormX() {
		return SmootherPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return SmootherPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormZ() {
		return DerivativeSmootherPolynomial.FORM;
	}
}