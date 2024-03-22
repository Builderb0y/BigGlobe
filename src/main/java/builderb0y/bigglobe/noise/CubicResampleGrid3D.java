package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;

import static builderb0y.bigglobe.math.Interpolator.*;

public class CubicResampleGrid3D extends ResampleGrid3D {

	public static final double
		MIN_OVERSHOOT = -125.0D / 64.0D * 0.5D + 0.5D,
		MAX_OVERSHOOT = +125.0D / 64.0D * 0.5D + 0.5D;

	public final transient double minValue, maxValue;

	public CubicResampleGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
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
	public double doInterpolateX(long seed, int x, int y, int z, double fracX) {
		return mixCubic(
			this.source.getValue(seed, x - this.scaleX, y, z),
			this.source.getValue(seed, x, y, z),
			this.source.getValue(seed, x + this.scaleX, y, z),
			this.source.getValue(seed, x + (this.scaleX << 1), y, z),
			fracX
		);
	}

	@Override
	public double doInterpolateY(long seed, int x, int y, int z, double fracY) {
		return mixCubic(
			this.source.getValue(seed, x, y - this.scaleY, z),
			this.source.getValue(seed, x, y, z),
			this.source.getValue(seed, x, y + this.scaleY, z),
			this.source.getValue(seed, x, y + (this.scaleY << 1), z),
			fracY
		);
	}

	@Override
	public double doInterpolateZ(long seed, int x, int y, int z, double fracZ) {
		return mixCubic(
			this.source.getValue(seed, x, y, z - this.scaleZ),
			this.source.getValue(seed, x, y, z),
			this.source.getValue(seed, x, y, z + this.scaleZ),
			this.source.getValue(seed, x, y, z + (this.scaleZ << 1)),
			fracZ
		);
	}

	@Override
	public double doInterpolateXY(long seed, int x, int y, int z, double fracX, double fracY) {
		return mixCubic(
			this.doInterpolateX(seed, x, y - this.scaleY, z, fracX),
			this.doInterpolateX(seed, x, y, z, fracX),
			this.doInterpolateX(seed, x, y + this.scaleY, z, fracX),
			this.doInterpolateX(seed, x, y + (this.scaleY << 1), z, fracX),
			fracY
		);
	}

	@Override
	public double doInterpolateYZ(long seed, int x, int y, int z, double fracY, double fracZ) {
		return mixCubic(
			this.doInterpolateY(seed, x, y, z - this.scaleZ, fracY),
			this.doInterpolateY(seed, x, y, z, fracY),
			this.doInterpolateY(seed, x, y, z + this.scaleZ, fracY),
			this.doInterpolateY(seed, x, y, z + (this.scaleZ << 1), fracY),
			fracZ
		);
	}

	@Override
	public double doInterpolateXZ(long seed, int x, int y, int z, double fracX, double fracZ) {
		return mixCubic(
			this.doInterpolateX(seed, x, y, z - this.scaleZ, fracX),
			this.doInterpolateX(seed, x, y, z, fracX),
			this.doInterpolateX(seed, x, y, z + this.scaleZ, fracX),
			this.doInterpolateX(seed, x, y, z + (this.scaleZ << 1), fracX),
			fracZ
		);
	}

	@Override
	public double doInterpolateXYZ(long seed, int x, int y, int z, double fracX, double fracY, double fracZ) {
		return mixCubic(
			this.doInterpolateXY(seed, x, y, z - this.scaleZ, fracX, fracY),
			this.doInterpolateXY(seed, x, y, z, fracX, fracY),
			this.doInterpolateXY(seed, x, y, z + this.scaleZ, fracX, fracY),
			this.doInterpolateXY(seed, x, y, z + (this.scaleZ << 1), fracX, fracY),
			fracZ
		);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX    = this.scaleX;
		double rcpX   = this.rcpX;
		int fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		int fracY     = BigGlobeMath.modulus_BP(y, this.scaleY);
		int fracZ     = BigGlobeMath.modulus_BP(z, this.scaleZ);
		int gridX     = startX - fracX;
		int gridY     = y      - fracY;
		int gridZ     = z      - fracZ;
		double curveY = this.curveY(fracY);
		double curveZ = this.curveZ(fracZ);
		double a      = this.checkInterpolateX(seed, gridX -  scaleX, gridY, gridZ, curveY, curveZ);
		double b      = this.checkInterpolateX(seed, gridX,           gridY, gridZ, curveY, curveZ);
		double c      = this.checkInterpolateX(seed, gridX += scaleX, gridY, gridZ, curveY, curveZ);
		double d      = this.checkInterpolateX(seed, gridX += scaleX, gridY, gridZ, curveY, curveZ);
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
				d     = this.checkInterpolateX(seed, gridX += scaleX, gridY, gridZ, curveY, curveZ);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleY    = this.scaleY;
		double rcpY   = this.rcpY;
		int fracX     = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		int fracZ     = BigGlobeMath.modulus_BP(z, this.scaleZ);
		int gridX     = x      - fracX;
		int gridY     = startY - fracY;
		int gridZ     = z      - fracZ;
		double curveX = this.curveX(fracX);
		double curveZ = this.curveZ(fracZ);
		double a      = this.checkInterpolateY(seed, gridX, gridY -  scaleY, gridZ, curveX, curveZ);
		double b      = this.checkInterpolateY(seed, gridX, gridY,           gridZ, curveX, curveZ);
		double c      = this.checkInterpolateY(seed, gridX, gridY += scaleY, gridZ, curveX, curveZ);
		double d      = this.checkInterpolateY(seed, gridX, gridY += scaleY, gridZ, curveX, curveZ);
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
				d     = this.checkInterpolateY(seed, gridX, gridY += scaleY, gridZ, curveX, curveZ);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleZ    = this.scaleZ;
		double rcpZ   = this.rcpZ;
		int fracX     = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY     = BigGlobeMath.modulus_BP(y, this.scaleY);
		int fracZ     = BigGlobeMath.modulus_BP(startZ, scaleZ);
		int gridX     = x      - fracX;
		int gridY     = y      - fracY;
		int gridZ     = startZ - fracZ;
		double curveX = this.curveX(fracX);
		double curveY = this.curveY(fracY);
		double a      = this.checkInterpolateZ(seed, gridX, gridY, gridZ -  scaleZ, curveX, curveY);
		double b      = this.checkInterpolateZ(seed, gridX, gridY, gridZ,           curveX, curveY);
		double c      = this.checkInterpolateZ(seed, gridX, gridY, gridZ += scaleZ, curveX, curveY);
		double d      = this.checkInterpolateZ(seed, gridX, gridY, gridZ += scaleZ, curveX, curveY);
		double term1  = cubicTerm1(a, b, c, d);
		double term2  = cubicTerm2(a, b, c, d);
		double term3  = cubicTerm3(a, b, c, d);
		double term4  = cubicTerm4(a, b, c, d);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracZ == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracZ * rcpZ));
			if (++index >= sampleCount) break;
			if (++fracZ >= scaleZ) {
				fracZ = 0;
				a     = b;
				b     = c;
				c     = d;
				d     = this.checkInterpolateZ(seed, gridX, gridY, gridZ += scaleZ, curveX, curveY);
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

	@Override
	public double curveZ(int fracZ) {
		return fracZ * this.rcpZ;
	}
}