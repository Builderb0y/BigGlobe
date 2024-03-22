package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;

import static builderb0y.bigglobe.math.Interpolator.mixLinear;

/** a ResampleGrid2D which internally interpolates between 4 sample points. */
public abstract class Resample4Grid2D extends ResampleGrid2D {

	public Resample4Grid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public double doInterpolateY(long seed, int x, int y0, double fracY) {
		return mixLinear(
			this.source.getValue(seed, x, y0),
			this.source.getValue(seed, x, y0 + this.scaleY),
			fracY
		);
	}

	@Override
	public double doInterpolateX(long seed, int x0, int y, double fracX) {
		return mixLinear(
			this.source.getValue(seed, x0, y),
			this.source.getValue(seed, x0 + this.scaleX, y),
			fracX
		);
	}

	@Override
	public double doInterpolateXY(long seed, int x0, int y0, double fracX, double fracY) {
		return mixLinear(
			this.doInterpolateY(seed, x0, y0, fracY),
			this.doInterpolateY(seed, x0 + this.scaleX, y0, fracY),
			fracX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX    = this.scaleX;
		int fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		int fracY     = BigGlobeMath.modulus_BP(y, this.scaleY);
		int gridX     = startX - fracX;
		int gridY     = y      - fracY;
		double curveY = this.curveY(fracY);
		double value0 = this.checkInterpolateX(seed, gridX,           gridY, curveY);
		double value1 = this.checkInterpolateX(seed, gridX += scaleX, gridY, curveY);
		double diff   = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? value0 : this.curveX(fracX) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracX >= scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = this.checkInterpolateX(seed, gridX += scaleX, gridY, curveY);
				diff   = value1 - value0;
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleY    = this.scaleY;
		int fracX     = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		int gridX     = x      - fracX;
		int gridY     = startY - fracY;
		double curveX = this.curveX(fracX);
		double value0 = this.checkInterpolateY(seed, gridX, gridY, curveX);
		double value1 = this.checkInterpolateY(seed, gridX, gridY += scaleY, curveX);
		double diff   = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracY == 0 ? value0 : this.curveY(fracY) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracY >= scaleY) {
				fracY  = 0;
				value0 = value1;
				value1 = this.checkInterpolateY(seed, gridX, gridY += scaleY, curveX);
				diff   = value1 - value0;
			}
		}
	}
}