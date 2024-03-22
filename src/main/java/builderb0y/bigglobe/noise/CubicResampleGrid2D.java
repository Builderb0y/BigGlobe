package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;

import static builderb0y.bigglobe.math.Interpolator.*;

public class CubicResampleGrid2D extends ResampleGrid2D {

	public static final double
		MIN_OVERSHOOT = -25.0D / 16.0D * 0.5D + 0.5D,
		MAX_OVERSHOOT = +25.0D / 16.0D * 0.5D + 0.5D;

	public final transient double minValue, maxValue;

	public CubicResampleGrid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
		this.minValue = mixLinear(source.minValue(), source.maxValue(), MAX_OVERSHOOT);
		this.maxValue = mixLinear(source.minValue(), source.maxValue(), MIN_OVERSHOOT);
	}

	@Override
	public double minValue() {
		return this.minValue;
	}

	@Override
	public double maxValue() {
		return this.maxValue;
	}

	@Override
	public double doInterpolateY(long seed, int x, int y0, double fracY) {
		return mixCubic(
			this.source.getValue(seed, x, y0 - this.scaleY),
			this.source.getValue(seed, x, y0),
			this.source.getValue(seed, x, y0 + this.scaleY),
			this.source.getValue(seed, x, y0 + (this.scaleY << 1)),
			fracY
		);
	}

	@Override
	public double doInterpolateX(long seed, int x0, int y, double fracX) {
		return mixCubic(
			this.source.getValue(seed, x0 - this.scaleX, y),
			this.source.getValue(seed, x0, y),
			this.source.getValue(seed, x0 + this.scaleX, y),
			this.source.getValue(seed, x0 + (this.scaleX << 1), y),
			fracX
		);
	}

	@Override
	public double doInterpolateXY(long seed, int x0, int y0, double fracX, double fracY) {
		return mixCubic(
			this.doInterpolateY(seed, x0 - this.scaleX, y0, fracY),
			this.doInterpolateY(seed, x0, y0, fracY),
			this.doInterpolateY(seed, x0 + this.scaleX, y0, fracY),
			this.doInterpolateY(seed, x0 + (this.scaleX << 1), y0, fracY),
			fracX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX    = this.scaleX;
		double rcpX   = this.rcpX;
		int fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		int fracY     = BigGlobeMath.modulus_BP(y, this.scaleY);
		int gridX     = startX - fracX;
		int gridY     = y      - fracY;
		double curveY = fracY * this.rcpY;
		double a      = this.checkInterpolateX(seed, gridX -  scaleX, gridY, curveY);
		double b      = this.checkInterpolateX(seed, gridX,           gridY, curveY);
		double c      = this.checkInterpolateX(seed, gridX += scaleX, gridY, curveY);
		double d      = this.checkInterpolateX(seed, gridX += scaleX, gridY, curveY);
		double term1  = cubicTerm1(a, b, c, d);
		double term2  = cubicTerm2(a, b, c, d);
		double term3  = cubicTerm3(a, b, c, d);
		double term4  = cubicTerm4(a, b, c, d);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracX * rcpX));
			if (++index >= sampleCount) break;
			if (++fracX >= scaleX) {
				fracX = 0;
				a     = b;
				b     = c;
				c     = d;
				d     = this.checkInterpolateX(seed, gridX += scaleX, gridY, curveY);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int    scaleY = this.scaleY;
		double rcpY   = this.rcpY;
		int    fracX  = BigGlobeMath.modulus_BP(x, this.scaleX);
		int    fracY  = BigGlobeMath.modulus_BP(startY, scaleY);
		int    gridX  = x - fracX;
		int    gridY  = startY - fracY;
		double curveX = fracX * this.rcpX;
		double a      = this.checkInterpolateY(seed, gridX, gridY - scaleY, curveX);
		double b      = this.checkInterpolateY(seed, gridX, gridY, curveX);
		double c      = this.checkInterpolateY(seed, gridX, gridY + scaleY, curveX);
		double d      = this.checkInterpolateY(seed, gridX, gridY + (scaleY << 1), curveX);
		double term1  = cubicTerm1(a, b, c, d);
		double term2  = cubicTerm2(a, b, c, d);
		double term3  = cubicTerm3(a, b, c, d);
		double term4  = cubicTerm4(a, b, c, d);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracY == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracY * rcpY));
			if (++index >= sampleCount) break;
			if (++fracY >= scaleY) {
				fracY = 0;
				a     = b;
				b     = c;
				c     = d;
				d     = this.checkInterpolateY(seed, gridX, gridY += scaleY, curveX);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public double curveX(int fracX) {
		return fracX * this.rcpX;
	}

	@Override
	public double curveY(int fracY) {
		return fracY * this.rcpY;
	}
}