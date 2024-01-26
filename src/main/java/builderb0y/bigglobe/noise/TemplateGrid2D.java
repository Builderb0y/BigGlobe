package builderb0y.bigglobe.noise;

import net.minecraft.registry.entry.RegistryEntry;

public class TemplateGrid2D implements Grid2D {

	public final RegistryEntry<Grid2D> template;

	public TemplateGrid2D(RegistryEntry<Grid2D> template) {
		this.template = template;
	}

	@Override
	public double minValue() {
		return this.template.value().minValue();
	}

	@Override
	public double maxValue() {
		return this.template.value().maxValue();
	}

	@Override
	public double getValue(long seed, int x, int y) {
		return this.template.value().getValue(seed, x, y);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		this.template.value().getBulkX(seed, startX, y, samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		this.template.value().getBulkY(seed, x, startY, samples);
	}
}