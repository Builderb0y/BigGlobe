package builderb0y.bigglobe.noise;

public class LinearResampleGrid1D extends Resample2Grid1D {

	public LinearResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public double curveX(int fracX) {
		return fracX * this.rcpX;
	}
}