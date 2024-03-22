package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;

public abstract class ResampleGrid2D implements Grid2D {

	public final Grid2D source;
	public final @VerifyIntRange(min = 0, minInclusive = false) int scaleX, scaleY;
	public final transient double rcpX, rcpY;

	public ResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		this.source = source;
		this.rcpX = 1.0D / (this.scaleX = scaleX);
		this.rcpY = 1.0D / (this.scaleY = scaleY);
	}

	@Override
	public double getValue(long seed, int x, int y) {
		int fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY = BigGlobeMath.modulus_BP(y, this.scaleY);
		return (
			fracX == 0
			? (
				fracY == 0
				? this.source.getValue(seed, x, y)
				: this.doInterpolateY(seed, x, y - fracY, this.curveY(fracY))
			)
			: (
				fracY == 0
				? this.doInterpolateX(seed, x - fracX, y, this.curveX(fracX))
				: this.doInterpolateXY(seed, x - fracX, y - fracY, this.curveX(fracX), this.curveY(fracY))
			)
		);
	}

	public double checkInterpolateX(long seed, int x, int y0, double fracY) {
		return (
			fracY == 0.0D
			? this.source.getValue(seed, x, y0)
			: this.doInterpolateY(seed, x, y0, fracY)
		);
	}

	public double checkInterpolateY(long seed, int x0, int y, double fracX) {
		return (
			fracX == 0.0D
			? this.source.getValue(seed, x0, y)
			: this.doInterpolateX(seed, x0, y, fracX)
		);
	}

	public abstract double doInterpolateY(long seed, int x, int y0, double fracY);

	public abstract double doInterpolateX(long seed, int x0, int y, double fracX);

	public abstract double doInterpolateXY(long seed, int x0, int y0, double fracX, double fracY);

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
}