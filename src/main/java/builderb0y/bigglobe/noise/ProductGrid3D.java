package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifySizeRange;

public class ProductGrid3D extends ProductGrid implements LayeredGrid3D {

	public final Grid3D @VerifySizeRange(min = 2) [] layers;

	public ProductGrid3D(Grid3D... layers) {
		super(layers);
		this.layers = layers;
	}

	@Override
	public Grid3D[] getLayers() {
		return this.layers;
	}
}