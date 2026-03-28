package builderb0y.bigglobe.noise;

import net.minecraft.core.Holder;

public class TemplateGrid1D implements Grid1D {

	public final Holder<Grid1D> template;

	public TemplateGrid1D(Holder<Grid1D> template) {
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
	public double getValue(long seed, int x) {
		return this.template.value().getValue(seed, x);
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		this.template.value().getBulkX(seed, startX, samples);
	}
}