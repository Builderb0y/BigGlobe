package builderb0y.bigglobe.noise;

public class LinearResampleGrid2D extends Resample4Grid2D {

	public LinearResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public double curveX(int fracX) {
		return fracX * this.rcpX;
	}

	@Override
	public double curveY(int fracY) {
		return fracY * this.rcpY;
	}
}