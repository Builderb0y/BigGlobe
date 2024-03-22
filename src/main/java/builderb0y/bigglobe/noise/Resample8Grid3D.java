package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;

import static builderb0y.bigglobe.math.Interpolator.mixLinear;

/** a ResampleGrid3D which internally interpolates between 8 sample points. */
public abstract class Resample8Grid3D extends ResampleGrid3D {

	public Resample8Grid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public double doInterpolateX(long seed, int x, int y, int z, double fracX) {
		return mixLinear(
			this.source.getValue(seed, x, y, z),
			this.source.getValue(seed, x + this.scaleX, y, z),
			fracX
		);
	}

	@Override
	public double doInterpolateY(long seed, int x, int y, int z, double fracY) {
		return mixLinear(
			this.source.getValue(seed, x, y, z),
			this.source.getValue(seed, x, y + this.scaleY, z),
			fracY
		);
	}

	@Override
	public double doInterpolateZ(long seed, int x, int y, int z, double fracZ) {
		return mixLinear(
			this.source.getValue(seed, x, y, z),
			this.source.getValue(seed, x, y, z + this.scaleZ),
			fracZ
		);
	}

	@Override
	public double doInterpolateXY(long seed, int x, int y, int z, double fracX, double fracY) {
		return mixLinear(
			this.doInterpolateX(seed, x, y, z, fracX),
			this.doInterpolateX(seed, x, y + this.scaleY, z, fracX),
			fracY
		);
	}

	@Override
	public double doInterpolateYZ(long seed, int x, int y, int z, double fracY, double fracZ) {
		return mixLinear(
			this.doInterpolateY(seed, x, y, z, fracY),
			this.doInterpolateY(seed, x, y, z + this.scaleZ, fracY),
			fracZ
		);
	}

	@Override
	public double doInterpolateXZ(long seed, int x, int y, int z, double fracX, double fracZ) {
		return mixLinear(
			this.doInterpolateX(seed, x, y, z, fracX),
			this.doInterpolateX(seed, x, y, z + this.scaleZ, fracX),
			fracZ
		);
	}

	@Override
	public double doInterpolateXYZ(long seed, int x, int y, int z, double fracX, double fracY, double fracZ) {
		return mixLinear(
			this.doInterpolateXY(seed, x, y, z, fracX, fracY),
			this.doInterpolateXY(seed, x, y, z + this.scaleZ, fracX, fracY),
			fracZ
		);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX    = this.scaleX;
		int fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		int fracY     = BigGlobeMath.modulus_BP(y, this.scaleY);
		int fracZ     = BigGlobeMath.modulus_BP(z, this.scaleZ);
		int gridX     = startX - fracX;
		int gridY     = y      - fracY;
		int gridZ     = z      - fracZ;
		double curveY = this.curveY(fracY);
		double curveZ = this.curveZ(fracZ);
		double value0 = this.checkInterpolateX(seed, gridX,           gridY, gridZ, curveY, curveZ);
		double value1 = this.checkInterpolateX(seed, gridX += scaleX, gridY, gridZ, curveY, curveZ);
		double diff   = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? value0 : this.curveX(fracX) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracX >= scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = this.checkInterpolateX(seed, gridX += scaleX, gridY, gridZ, curveY, curveZ);
				diff   = value1 - value0;
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleY    = this.scaleY;
		int fracX     = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY     = BigGlobeMath.modulus_BP(startY, scaleY);
		int fracZ     = BigGlobeMath.modulus_BP(z, this.scaleZ);
		int gridX     = x      - fracX;
		int gridY     = startY - fracY;
		int gridZ     = z      - fracZ;
		double curveX = this.curveX(fracX);
		double curveZ = this.curveZ(fracZ);
		double value0 = this.checkInterpolateY(seed, gridX, gridY, gridZ, curveX, curveZ);
		double value1 = this.checkInterpolateY(seed, gridX, gridY += scaleY, gridZ, curveX, curveZ);
		double diff   = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracY == 0 ? value0 : this.curveY(fracY) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracY >= scaleY) {
				fracY  = 0;
				value0 = value1;
				value1 = this.checkInterpolateY(seed, gridX, gridY += scaleY, gridZ, curveX, curveZ);
				diff   = value1 - value0;
			}
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleZ    = this.scaleZ;
		int fracX     = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY     = BigGlobeMath.modulus_BP(y, this.scaleY);
		int fracZ     = BigGlobeMath.modulus_BP(startZ, scaleZ);
		int gridX     = x      - fracX;
		int gridY     = y      - fracY;
		int gridZ     = startZ - fracZ;
		double curveX = this.curveX(fracX);
		double curveY = this.curveY(fracY);
		double value0 = this.checkInterpolateZ(seed, gridX, gridY, gridZ, curveX, curveY);
		double value1 = this.checkInterpolateZ(seed, gridX, gridY, gridZ += scaleZ, curveX, curveY);
		double diff   = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracZ == 0 ? value0 : this.curveZ(fracZ) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracZ >= scaleZ) {
				fracZ  = 0;
				value0 = value1;
				value1 = this.checkInterpolateZ(seed, gridX, gridY, gridZ += scaleZ, curveX, curveY);
				diff   = value1 - value0;
			}
		}
	}
}