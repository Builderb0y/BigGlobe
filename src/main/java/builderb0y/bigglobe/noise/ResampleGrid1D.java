package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.polynomials.Polynomial.PolyForm;

public abstract class ResampleGrid1D implements Grid1D {

	public final Grid1D source;
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX;
	public final transient double rcpX;
	public final transient double minValue, maxValue;

	public ResampleGrid1D(Grid1D source, int scaleX) {
		this.source = source;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.minValue = Interpolator.mixLinear(source.maxValue(), source.minValue(), this.polyForm().getMaxOvershoot());
		this.maxValue = Interpolator.mixLinear(source.minValue(), source.maxValue(), this.polyForm().getMaxOvershoot());
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