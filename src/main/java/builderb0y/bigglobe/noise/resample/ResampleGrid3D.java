package builderb0y.bigglobe.noise.resample;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.polynomials.Polynomial.PolyForm;

public abstract class ResampleGrid3D implements Grid3D {

	public final Grid3D source;
	public final @VerifyIntRange(min = 0, minInclusive = false) @Alias("scale") int scaleX, scaleY, scaleZ;
	public final transient double rcpX, rcpY, rcpZ;
	public final transient double minValue, maxValue;

	public ResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		this.source = source;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.rcpY = 1.0D / (this.scaleY = scaleY);
		this.rcpZ = 1.0D / (this.scaleZ = scaleZ);
		double minZ = this.polyFormZ().calcMinValue(source.minValue(), source.maxValue(), this.rcpZ);
		double maxZ = this.polyFormZ().calcMaxValue(source.minValue(), source.maxValue(), this.rcpZ);
		double minY = this.polyFormY().calcMinValue(minZ, maxZ, this.rcpY);
		double maxY = this.polyFormY().calcMaxValue(minZ, maxZ, this.rcpY);
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

	public abstract PolyForm polyFormZ();
}