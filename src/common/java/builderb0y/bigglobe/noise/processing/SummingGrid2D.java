package builderb0y.bigglobe.noise.processing;

import builderb0y.autocodec.annotations.VerifySizeRange;
import builderb0y.bigglobe.noise.Grid2D;

public class SummingGrid2D extends SummingGrid implements LayeredGrid2D {

	public final Grid2D @VerifySizeRange(min = 2) [] layers;

	public SummingGrid2D(Grid2D... layers) {
		super(layers);
		this.layers = layers;
	}

	@Override
	public Grid2D[] getLayers() {
		return this.layers;
	}
}