package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.polynomials.Polynomial.PolyForm;

public abstract class ResampleGrid3D implements Grid3D {

	public final Grid3D source;
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX, scaleY, scaleZ;
	public final transient double rcpX, rcpY, rcpZ;
	public final transient double minValue, maxValue;

	public ResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		this.source = source;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.rcpY = 1.0D / (this.scaleY = scaleY);
		this.rcpZ = 1.0D / (this.scaleZ = scaleZ);
		double overshoot = this.polyFormX().getMaxOvershoot() * this.polyFormY().getMaxOvershoot() * this.polyFormZ().getMaxOvershoot();
		this.minValue = Interpolator.mixLinear(source.maxValue(), source.minValue(), overshoot);
		this.maxValue = Interpolator.mixLinear(source.minValue(), source.maxValue(), overshoot);
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

	public abstract PolyForm polyFormZ();
}