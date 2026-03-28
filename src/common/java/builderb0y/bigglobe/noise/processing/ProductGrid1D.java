package builderb0y.bigglobe.noise.processing;

import builderb0y.autocodec.annotations.VerifySizeRange;
import builderb0y.bigglobe.noise.Grid1D;

public class ProductGrid1D extends ProductGrid implements LayeredGrid1D {

	public final Grid1D @VerifySizeRange(min = 2) [] layers;

	public ProductGrid1D(Grid1D... layers) {
		super(layers);
		this.layers = layers;
	}

	@Override
	public Grid1D[] getLayers() {
		return this.layers;
	}
}