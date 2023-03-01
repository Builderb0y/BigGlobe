package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifySizeRange;

public class SummingGrid1D extends SummingGrid implements LayeredGrid1D {

	public final Grid1D @VerifySizeRange(min = 2) [] layers;

	public SummingGrid1D(Grid1D... layers) {
		super(layers);
		this.layers = layers;
	}

	@Override
	public Grid1D[] getLayers() {
		return this.layers;
	}
}