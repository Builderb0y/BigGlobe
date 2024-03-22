package builderb0y.bigglobe.noise;

public class LinearResampleGrid3D extends Resample8Grid3D {

	public LinearResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public double curveX(int fracX) {
		return fracX * this.rcpX;
	}

	@Override
	public double curveY(int fracY) {
		return fracY * this.rcpY;
	}

	@Override
	public double curveZ(int fracZ) {
		return fracZ * this.rcpZ;
	}
}