package builderb0y.bigglobe.noise.resample;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.polynomials.Polynomial.PolyForm;

public abstract class ResampleGrid1D implements Grid1D {

	public final Grid1D source;
	public final @VerifyIntRange(min = 0, minInclusive = false) @Alias("scale") int scaleX;
	public final transient double rcpX;
	public final transient double minValue, maxValue;

	public ResampleGrid1D(Grid1D source, int scaleX) {
		this.source = source;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.minValue = this.polyForm().calcMinValue(source.minValue(), source.maxValue(), this.rcpX);
		this.maxValue = this.polyForm().calcMaxValue(source.minValue(), source.maxValue(), this.rcpX);
	}

	@Override
	public double minValue() {
		return this.minValue;
	}

	@Override
	public double maxValue() {
		return this.maxValue;
	}

	public abstract PolyForm polyForm();
}