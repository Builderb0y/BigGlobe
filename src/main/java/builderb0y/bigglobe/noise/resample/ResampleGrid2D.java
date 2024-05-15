package builderb0y.bigglobe.noise.resample;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.polynomials.Polynomial.PolyForm;

public abstract class ResampleGrid2D implements Grid2D {

	public final Grid2D source;
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX, scaleY;
	public final transient double rcpX, rcpY;
	public final transient double minValue, maxValue;

	public ResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		this.source = source;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.rcpY = 1.0D / (this.scaleY = scaleY);
		double minY = this.polyFormY().calcMinValue(source.minValue(), source.maxValue(), this.rcpY);
		double maxY = this.polyFormY().calcMaxValue(source.minValue(), source.maxValue(), this.rcpY);
		this.minValue = this.polyFormX().calcMinValue(minY, maxY, this.rcpX);
		this.maxValue = this.polyFormX().calcMaxValue(minY, maxY, this.rcpX);
	}

	@Override
	public double minValue() {
		return this.minValue;
	}

	@Override
	public double maxValue() {
		return this.maxValue;
	}

	public abstract PolyForm polyFormX();

	public abstract PolyForm polyFormY();
}