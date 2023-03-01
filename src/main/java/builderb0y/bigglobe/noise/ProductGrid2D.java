package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifySizeRange;

public class ProductGrid2D extends ProductGrid implements LayeredGrid2D {

	public final Grid2D @VerifySizeRange(min = 2) [] layers;

	public ProductGrid2D(Grid2D... layers) {
		super(layers);
		this.layers = layers;
	}

	@Override
	public Grid2D[] getLayers() {
		return this.layers;
	}
}