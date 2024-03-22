package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;

public abstract class ResampleGrid3D implements Grid3D {

	public final Grid3D source;
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX, scaleY, scaleZ;
	public final transient double rcpX, rcpY, rcpZ;

	public ResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		this.source = source;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.rcpY = 1.0D / (this.scaleY = scaleY);
		this.rcpZ = 1.0D / (this.scaleZ = scaleZ);
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		int fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY = BigGlobeMath.modulus_BP(y, this.scaleY);
		int fracZ = BigGlobeMath.modulus_BP(z, this.scaleZ);
		return (
			fracX == 0
			? (
				fracY == 0
				? (
					fracZ == 0
					? this.source.getValue(seed, x, y, z)
					: this.doInterpolateZ(seed, x, y, z - fracZ, this.curveZ(fracZ))
				)
				: (
					fracZ == 0
					? this.doInterpolateY(seed, x, y - fracY, z, this.curveY(fracY))
					: this.doInterpolateYZ(seed, x, y - fracY, z - fracZ, this.curveY(fracY), this.curveZ(fracZ))
				)
			)
			: (
				fracY == 0
				? (
					fracZ == 0
					? this.doInterpolateX(seed, x - fracX, y, z, this.curveX(fracX))
					: this.doInterpolateXZ(seed, x - fracX, y, z - fracZ, this.curveX(fracX), this.curveZ(fracZ))
				)
				: (
					fracZ == 0
					? this.doInterpolateXY(seed, x - fracX, y - fracY, z, this.curveX(fracX), this.curveY(fracY))
					: this.doInterpolateXYZ(seed, x - fracX, y - fracY, z - fracZ, this.curveX(fracX), this.curveY(fracY), this.curveZ(fracZ))
				)
			)
		);
	}

	public double checkInterpolateX(long seed, int x, int y, int z, double fracY, double fracZ) {
		return (
			fracY == 0.0D
			? (
				fracZ == 0.0D
				? this.source.getValue(seed, x, y, z)
				: this.doInterpolateZ(seed, x, y, z, fracZ)
			)
			: (
				fracZ == 0.0D
				? this.doInterpolateY(seed, x, y, z, fracY)
				: this.doInterpolateYZ(seed, x, y, z, fracY, fracZ)
			)
		);
	}

	public double checkInterpolateY(long seed, int x, int y, int z, double fracX, double fracZ) {
		return (
			fracX == 0.0D
			? (
				fracZ == 0.0D
				? this.source.getValue(seed, x, y, z)
				: this.doInterpolateZ(seed, x, y, z, fracZ)
			)
			: (
				fracZ == 0.0D
				? this.doInterpolateX(seed, x, y, z, fracX)
				: this.doInterpolateXZ(seed, x, y, z, fracX, fracZ)
			)
		);
	}

	public double checkInterpolateZ(long seed, int x, int y, int z, double fracX, double fracY) {
		return (
			fracX == 0.0D
			? (
				fracY == 0.0D
				? this.source.getValue(seed, x, y, z)
				: this.doInterpolateY(seed, x, y, z, fracY)
			)
			: (
				fracY == 0.0D
				? this.doInterpolateX(seed, x, y, z, fracX)
				: this.doInterpolateXY(seed, x, y, z, fracX, fracY)
			)
		);
	}

	public abstract double doInterpolateX(long seed, int x, int y, int z, double fracX);

	public abstract double doInterpolateY(long seed, int x, int y, int z, double fracY);

	public abstract double doInterpolateZ(long seed, int x, int y, int z, double fracZ);

	public abstract double doInterpolateXY(long seed, int x, int y, int z, double fracX, double fracY);

	public abstract double doInterpolateYZ(long seed, int x, int y, int z, double fracY, double fracZ);

	public abstract double doInterpolateXZ(long seed, int x, int y, int z, double fracX, double fracZ);

	public abstract double doInterpolateXYZ(long seed, int x, int y, int z, double fracX, double fracY, double fracZ);

	@Override
	public double minValue() {
		return this.source.minValue();
	}

	@Override
	public double maxValue() {
		return this.source.maxValue();
	}

	public abstract double curveX(int fracX);

	public abstract double curveY(int fracY);

	public abstract double curveZ(int fracZ);
}