package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmoothPolynomial;

public class SmoothResampleGrid3D extends Resample8Grid3D {

	public SmoothResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public PolyForm2 polyFormX() {
		return SmoothPolynomial.FORM;
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