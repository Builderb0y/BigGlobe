package builderb0y.bigglobe.noise.resample.derivatives;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.polynomials.SmootherDerivativePolynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmootherPolynomial;
import builderb0y.bigglobe.noise.resample.Resample8Grid3D;

public class SmootherDerivativeXResampleGrid3D extends Resample8Grid3D {

	public SmootherDerivativeXResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm2 polyFormX() {
		return SmootherDerivativePolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormY() {
		return SmootherPolynomial.FORM;
	}

	@Override
	public PolyForm2 polyFormZ() {
		return SmootherPolynomial.FORM;
	}
}