package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;

public abstract class ResampleGrid1D implements Grid1D {

	public final Grid1D source;
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX;
	public final transient double rcpX;

	public ResampleGrid1D(Grid1D source, int scaleX) {
		this.source = source;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
	}

	@Override
	public double minValue() {
		return this.source.minValue();
	}

	@Override
	public double maxValue() {
		return this.source.maxValue();
	}
}