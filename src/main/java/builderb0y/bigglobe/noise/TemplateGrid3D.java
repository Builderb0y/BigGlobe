package builderb0y.bigglobe.noise;

import net.minecraft.registry.entry.RegistryEntry;

public class TemplateGrid3D implements Grid3D {

	public final RegistryEntry<Grid3D> template;

	public TemplateGrid3D(RegistryEntry<Grid3D> template) {
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
	public double getValue(long seed, int x, int y, int z) {
		return this.template.value().getValue(seed, x, y, z);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		this.template.value().getBulkX(seed, startX, y, z, samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		this.template.value().getBulkY(seed, x, startY, z, samples);
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		this.template.value().getBulkZ(seed, x, y, startZ, samples);
	}
}