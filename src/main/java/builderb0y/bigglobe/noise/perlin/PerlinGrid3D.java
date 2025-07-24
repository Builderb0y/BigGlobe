package builderb0y.bigglobe.noise.perlin;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;

public class PerlinGrid3D extends PerlinBaseGrid3D {

	public PerlinGrid3D(Seed salt, int scaleX, int scaleY, int scaleZ, double max_slope, double max_offset) {
		super(salt, scaleX, scaleY, scaleZ, max_slope, max_offset);
	}

	@Override
	public double minValue() {
		return -(this.max_slope * 1.5D + this.max_offset);
	}

	@Override
	public double maxValue() {
		return this.max_slope * 1.5D + this.max_offset;
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		seed = this.salt.xor(seed);
		int
			modX    = BigGlobeMath.modulus_BP(x, this.scaleX),
			modY    = BigGlobeMath.modulus_BP(y, this.scaleY),
			modZ    = BigGlobeMath.modulus_BP(z, this.scaleZ),
			gridX0  = x - modX,
			gridY0  = y - modY,
			gridZ0  = z - modZ,
			gridX1  = gridX0 + this.scaleX,
			gridY1  = gridY0 + this.scaleY,
			gridZ1  = gridZ0 + this.scaleZ;
		double
			fracX0  = modX * this.rcpX,
			fracY0  = modY * this.rcpY,
			fracZ0  = modZ * this.rcpZ,
			fracX1  = fracX0 - 1.0D,
			fracY1  = fracY0 - 1.0D,
			fracZ1  = fracZ0 - 1.0D,
			smoothX = Interpolator.smooth(fracX0),
			smoothY = Interpolator.smooth(fracY0),
			smoothZ = Interpolator.smooth(fracZ0);
		return Interpolator.mixLinear(
			Interpolator.mixLinear(
				Interpolator.mixLinear(
					+ this.slopeX(seed, gridX0, gridY0, gridZ0) * fracX0
					+ this.slopeY(seed, gridX0, gridY0, gridZ0) * fracY0
					+ this.slopeZ(seed, gridX0, gridY0, gridZ0) * fracZ0
					+ this.offset(seed, gridX0, gridY0, gridZ0),
					+ this.slopeX(seed, gridX0, gridY0, gridZ1) * fracX0
					+ this.slopeY(seed, gridX0, gridY0, gridZ1) * fracY0
					+ this.slopeZ(seed, gridX0, gridY0, gridZ1) * fracZ1
					+ this.offset(seed, gridX0, gridY0, gridZ1),
					smoothZ
				),
				Interpolator.mixLinear(
					+ this.slopeX(seed, gridX0, gridY1, gridZ0) * fracX0
					+ this.slopeY(seed, gridX0, gridY1, gridZ0) * fracY1
					+ this.slopeZ(seed, gridX0, gridY1, gridZ0) * fracZ0
					+ this.offset(seed, gridX0, gridY1, gridZ0),
					+ this.slopeX(seed, gridX0, gridY1, gridZ1) * fracX0
					+ this.slopeY(seed, gridX0, gridY1, gridZ1) * fracY1
					+ this.slopeZ(seed, gridX0, gridY1, gridZ1) * fracZ1
					+ this.offset(seed, gridX0, gridY1, gridZ1),
					smoothZ
				),
				smoothY
			),
			Interpolator.mixLinear(
				Interpolator.mixLinear(
					+ this.slopeX(seed, gridX1, gridY0, gridZ0) * fracX1
					+ this.slopeY(seed, gridX1, gridY0, gridZ0) * fracY0
					+ this.slopeZ(seed, gridX1, gridY0, gridZ0) * fracZ0
					+ this.offset(seed, gridX1, gridY0, gridZ0),
					+ this.slopeX(seed, gridX1, gridY0, gridZ1) * fracX1
					+ this.slopeY(seed, gridX1, gridY0, gridZ1) * fracY0
					+ this.slopeZ(seed, gridX1, gridY0, gridZ1) * fracZ1
					+ this.offset(seed, gridX1, gridY0, gridZ1),
					smoothZ
				),
				Interpolator.mixLinear(
					+ this.slopeX(seed, gridX1, gridY1, gridZ0) * fracX1
					+ this.slopeY(seed, gridX1, gridY1, gridZ0) * fracY1
					+ this.slopeZ(seed, gridX1, gridY1, gridZ0) * fracZ0
					+ this.offset(seed, gridX1, gridY1, gridZ0),
					+ this.slopeX(seed, gridX1, gridY1, gridZ1) * fracX1
					+ this.slopeY(seed, gridX1, gridY1, gridZ1) * fracY1
					+ this.slopeZ(seed, gridX1, gridY1, gridZ1) * fracZ1
					+ this.offset(seed, gridX1, gridY1, gridZ1),
					smoothZ
				),
				smoothY
			),
			smoothX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX    = this.scaleX,
			scaleY    = this.scaleY,
			scaleZ    = this.scaleZ,
			modX      = BigGlobeMath.modulus_BP(startX, scaleX),
			modY      = BigGlobeMath.modulus_BP(y,      scaleY),
			modZ      = BigGlobeMath.modulus_BP(z,      scaleZ),
			gridX     = startX - modX,
			gridY0    = y      - modY,
			gridZ0    = z      - modZ,
			gridY1    = gridY0 + scaleY,
			gridZ1    = gridZ0 + scaleZ;
		double
			rcpX      = this.rcpX,
			fracY0    = this.rcpY * modY,
			fracZ0    = this.rcpZ * modZ,
			fracY1    = fracY0 - 1.0D,
			fracZ1    = fracZ0 - 1.0D,
			smoothY   = Interpolator.smooth(fracY0),
			smoothZ   = Interpolator.smooth(fracZ0),
			slopeX000 = this.slopeX(seed, gridX, gridY0, gridZ0),
			slopeX001 = this.slopeX(seed, gridX, gridY0, gridZ1),
			slopeX010 = this.slopeX(seed, gridX, gridY1, gridZ0),
			slopeX011 = this.slopeX(seed, gridX, gridY1, gridZ1),
			offset000 = this.slopeY(seed, gridX, gridY0, gridZ0) * fracY0 + this.slopeZ(seed, gridX, gridY0, gridZ0) * fracZ0 + this.offset(seed, gridX, gridY0, gridZ0),
			offset001 = this.slopeY(seed, gridX, gridY0, gridZ1) * fracY0 + this.slopeZ(seed, gridX, gridY0, gridZ1) * fracZ1 + this.offset(seed, gridX, gridY0, gridZ1),
			offset010 = this.slopeY(seed, gridX, gridY1, gridZ0) * fracY1 + this.slopeZ(seed, gridX, gridY1, gridZ0) * fracZ0 + this.offset(seed, gridX, gridY1, gridZ0),
			offset011 = this.slopeY(seed, gridX, gridY1, gridZ1) * fracY1 + this.slopeZ(seed, gridX, gridY1, gridZ1) * fracZ1 + this.offset(seed, gridX, gridY1, gridZ1);
		gridX += scaleX;
		double
			slopeX100 = this.slopeX(seed, gridX, gridY0, gridZ0),
			slopeX101 = this.slopeX(seed, gridX, gridY0, gridZ1),
			slopeX110 = this.slopeX(seed, gridX, gridY1, gridZ0),
			slopeX111 = this.slopeX(seed, gridX, gridY1, gridZ1),
			offset100 = this.slopeY(seed, gridX, gridY0, gridZ0) * fracY0 + this.slopeZ(seed, gridX, gridY0, gridZ0) * fracZ0 + this.offset(seed, gridX, gridY0, gridZ0),
			offset101 = this.slopeY(seed, gridX, gridY0, gridZ1) * fracY0 + this.slopeZ(seed, gridX, gridY0, gridZ1) * fracZ1 + this.offset(seed, gridX, gridY0, gridZ1),
			offset110 = this.slopeY(seed, gridX, gridY1, gridZ0) * fracY1 + this.slopeZ(seed, gridX, gridY1, gridZ0) * fracZ0 + this.offset(seed, gridX, gridY1, gridZ0),
			offset111 = this.slopeY(seed, gridX, gridY1, gridZ1) * fracY1 + this.slopeZ(seed, gridX, gridY1, gridZ1) * fracZ1 + this.offset(seed, gridX, gridY1, gridZ1);
		for (int index = 0; true /* break in the middle of the loop. */;) {
			double fracX0 = modX * rcpX;
			double fracX1 = fracX0 - 1.0D;
			samples.setD(
				index,
				Interpolator.mixSmoothUnchecked(
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeX000 * fracX0 + offset000,
							slopeX001 * fracX0 + offset001,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeX010 * fracX0 + offset010,
							slopeX011 * fracX0 + offset011,
							smoothZ
						),
						smoothY
					),
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeX100 * fracX1 + offset100,
							slopeX101 * fracX1 + offset101,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeX110 * fracX1 + offset110,
							slopeX111 * fracX1 + offset111,
							smoothZ
						),
						smoothY
					),
					fracX0
				)
			);
			if (++index  >= sampleCount) break;
			if (++modX   >= scaleX) {
				modX      = 0;
				slopeX000 = slopeX100;
				slopeX001 = slopeX101;
				slopeX010 = slopeX110;
				slopeX011 = slopeX111;
				offset000 = offset100;
				offset001 = offset101;
				offset010 = offset110;
				offset011 = offset111;
				gridX    += scaleX;
				slopeX100 = this.slopeX(seed, gridX, gridY0, gridZ0);
				slopeX101 = this.slopeX(seed, gridX, gridY0, gridZ1);
				slopeX110 = this.slopeX(seed, gridX, gridY1, gridZ0);
				slopeX111 = this.slopeX(seed, gridX, gridY1, gridZ1);
				offset100 = this.slopeY(seed, gridX, gridY0, gridZ0) * fracY0 + this.slopeZ(seed, gridX, gridY0, gridZ0) * fracZ0 + this.offset(seed, gridX, gridY0, gridZ0);
				offset101 = this.slopeY(seed, gridX, gridY0, gridZ1) * fracY0 + this.slopeZ(seed, gridX, gridY0, gridZ1) * fracZ1 + this.offset(seed, gridX, gridY0, gridZ1);
				offset110 = this.slopeY(seed, gridX, gridY1, gridZ0) * fracY1 + this.slopeZ(seed, gridX, gridY1, gridZ0) * fracZ0 + this.offset(seed, gridX, gridY1, gridZ0);
				offset111 = this.slopeY(seed, gridX, gridY1, gridZ1) * fracY1 + this.slopeZ(seed, gridX, gridY1, gridZ1) * fracZ1 + this.offset(seed, gridX, gridY1, gridZ1);
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX    = this.scaleX,
			scaleY    = this.scaleY,
			scaleZ    = this.scaleZ,
			modX      = BigGlobeMath.modulus_BP(x,      scaleX),
			modY      = BigGlobeMath.modulus_BP(startY, scaleY),
			modZ      = BigGlobeMath.modulus_BP(z,      scaleZ),
			gridY     = startY - modY,
			gridX0    = x      - modX,
			gridZ0    = z      - modZ,
			gridX1    = gridX0 + scaleX,
			gridZ1    = gridZ0 + scaleZ;
		double
			rcpY      = this.rcpY,
			fracX0    = this.rcpX * modX,
			fracZ0    = this.rcpZ * modZ,
			fracX1    = fracX0 - 1.0D,
			fracZ1    = fracZ0 - 1.0D,
			smoothX   = Interpolator.smooth(fracX0),
			smoothZ   = Interpolator.smooth(fracZ0),
			slopeY000 = this.slopeY(seed, gridX0, gridY, gridZ0),
			slopeY001 = this.slopeY(seed, gridX0, gridY, gridZ1),
			slopeY100 = this.slopeY(seed, gridX1, gridY, gridZ0),
			slopeY101 = this.slopeY(seed, gridX1, gridY, gridZ1),
			offset000 = this.slopeX(seed, gridX0, gridY, gridZ0) * fracX0 + this.slopeZ(seed, gridX0, gridY, gridZ0) * fracZ0 + this.offset(seed, gridX0, gridY, gridZ0),
			offset001 = this.slopeX(seed, gridX0, gridY, gridZ1) * fracX0 + this.slopeZ(seed, gridX0, gridY, gridZ1) * fracZ1 + this.offset(seed, gridX0, gridY, gridZ1),
			offset100 = this.slopeX(seed, gridX1, gridY, gridZ0) * fracX1 + this.slopeZ(seed, gridX1, gridY, gridZ0) * fracZ0 + this.offset(seed, gridX1, gridY, gridZ0),
			offset101 = this.slopeX(seed, gridX1, gridY, gridZ1) * fracX1 + this.slopeZ(seed, gridX1, gridY, gridZ1) * fracZ1 + this.offset(seed, gridX1, gridY, gridZ1);
		gridY += scaleY;
		double
			slopeY010 = this.slopeY(seed, gridX0, gridY, gridZ0),
			slopeY011 = this.slopeY(seed, gridX0, gridY, gridZ1),
			slopeY110 = this.slopeY(seed, gridX1, gridY, gridZ0),
			slopeY111 = this.slopeY(seed, gridX1, gridY, gridZ1),
			offset010 = this.slopeX(seed, gridX0, gridY, gridZ0) * fracX0 + this.slopeZ(seed, gridX0, gridY, gridZ0) * fracZ0 + this.offset(seed, gridX0, gridY, gridZ0),
			offset011 = this.slopeX(seed, gridX0, gridY, gridZ1) * fracX0 + this.slopeZ(seed, gridX0, gridY, gridZ1) * fracZ1 + this.offset(seed, gridX0, gridY, gridZ1),
			offset110 = this.slopeX(seed, gridX1, gridY, gridZ0) * fracX1 + this.slopeZ(seed, gridX1, gridY, gridZ0) * fracZ0 + this.offset(seed, gridX1, gridY, gridZ0),
			offset111 = this.slopeX(seed, gridX1, gridY, gridZ1) * fracX1 + this.slopeZ(seed, gridX1, gridY, gridZ1) * fracZ1 + this.offset(seed, gridX1, gridY, gridZ1);
		for (int index = 0; true /* break in the middle of the loop. */;) {
			double fracY0 = modY * rcpY;
			double fracY1 = fracY0 - 1.0D;
			samples.setD(
				index,
				Interpolator.mixSmoothUnchecked(
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeY000 * fracY0 + offset000,
							slopeY001 * fracY0 + offset001,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeY100 * fracY0 + offset100,
							slopeY101 * fracY0 + offset101,
							smoothZ
						),
						smoothX
					),
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeY010 * fracY1 + offset010,
							slopeY011 * fracY1 + offset011,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeY110 * fracY1 + offset110,
							slopeY111 * fracY1 + offset111,
							smoothZ
						),
						smoothX
					),
					fracY0
				)
			);
			if (++index  >= sampleCount) break;
			if (++modY   >= scaleY) {
				modY      = 0;
				slopeY000 = slopeY010;
				slopeY001 = slopeY011;
				slopeY100 = slopeY110;
				slopeY101 = slopeY111;
				offset000 = offset010;
				offset001 = offset011;
				offset100 = offset110;
				offset101 = offset111;
				gridY    += scaleY;
				slopeY010 = this.slopeY(seed, gridX0, gridY, gridZ0);
				slopeY011 = this.slopeY(seed, gridX0, gridY, gridZ1);
				slopeY110 = this.slopeY(seed, gridX1, gridY, gridZ0);
				slopeY111 = this.slopeY(seed, gridX1, gridY, gridZ1);
				offset010 = this.slopeX(seed, gridX0, gridY, gridZ0) * fracX0 + this.slopeZ(seed, gridX0, gridY, gridZ0) * fracZ0 + this.offset(seed, gridX0, gridY, gridZ0);
				offset011 = this.slopeX(seed, gridX0, gridY, gridZ1) * fracX0 + this.slopeZ(seed, gridX0, gridY, gridZ1) * fracZ1 + this.offset(seed, gridX0, gridY, gridZ1);
				offset110 = this.slopeX(seed, gridX1, gridY, gridZ0) * fracX1 + this.slopeZ(seed, gridX1, gridY, gridZ0) * fracZ0 + this.offset(seed, gridX1, gridY, gridZ0);
				offset111 = this.slopeX(seed, gridX1, gridY, gridZ1) * fracX1 + this.slopeZ(seed, gridX1, gridY, gridZ1) * fracZ1 + this.offset(seed, gridX1, gridY, gridZ1);
			}
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX    = this.scaleX,
			scaleY    = this.scaleY,
			scaleZ    = this.scaleZ,
			modX      = BigGlobeMath.modulus_BP(x,      scaleX),
			modY      = BigGlobeMath.modulus_BP(y,      scaleY),
			modZ      = BigGlobeMath.modulus_BP(startZ, scaleZ),
			gridZ     = startZ - modZ,
			gridX0    = x      - modX,
			gridY0    = y      - modY,
			gridX1    = gridX0 + scaleX,
			gridY1    = gridY0 + scaleY;
		double
			rcpZ      = this.rcpZ,
			fracX0    = this.rcpX * modX,
			fracY0    = this.rcpY * modY,
			fracX1    = fracX0 - 1.0D,
			fracY1    = fracY0 - 1.0D,
			smoothX   = Interpolator.smooth(fracX0),
			smoothY   = Interpolator.smooth(fracY0),
			slopeZ000 = this.slopeZ(seed, gridX0, gridY0, gridZ),
			slopeZ010 = this.slopeZ(seed, gridX0, gridY1, gridZ),
			slopeZ100 = this.slopeZ(seed, gridX1, gridY0, gridZ),
			slopeZ110 = this.slopeZ(seed, gridX1, gridY1, gridZ),
			offset000 = this.slopeX(seed, gridX0, gridY0, gridZ) * fracX0 + this.slopeY(seed, gridX0, gridY0, gridZ) * fracY0 + this.offset(seed, gridX0, gridY0, gridZ),
			offset010 = this.slopeX(seed, gridX0, gridY1, gridZ) * fracX0 + this.slopeY(seed, gridX0, gridY1, gridZ) * fracY1 + this.offset(seed, gridX0, gridY1, gridZ),
			offset100 = this.slopeX(seed, gridX1, gridY0, gridZ) * fracX1 + this.slopeY(seed, gridX1, gridY0, gridZ) * fracY0 + this.offset(seed, gridX1, gridY0, gridZ),
			offset110 = this.slopeX(seed, gridX1, gridY1, gridZ) * fracX1 + this.slopeY(seed, gridX1, gridY1, gridZ) * fracY1 + this.offset(seed, gridX1, gridY1, gridZ);
		gridZ += scaleZ;
		double
			slopeZ001 = this.slopeZ(seed, gridX0, gridY0, gridZ),
			slopeZ011 = this.slopeZ(seed, gridX0, gridY1, gridZ),
			slopeZ101 = this.slopeZ(seed, gridX1, gridY0, gridZ),
			slopeZ111 = this.slopeZ(seed, gridX1, gridY1, gridZ),
			offset001 = this.slopeX(seed, gridX0, gridY0, gridZ) * fracX0 + this.slopeY(seed, gridX0, gridY0, gridZ) * fracY0 + this.offset(seed, gridX0, gridY0, gridZ),
			offset011 = this.slopeX(seed, gridX0, gridY1, gridZ) * fracX0 + this.slopeY(seed, gridX0, gridY1, gridZ) * fracY1 + this.offset(seed, gridX0, gridY1, gridZ),
			offset101 = this.slopeX(seed, gridX1, gridY0, gridZ) * fracX1 + this.slopeY(seed, gridX1, gridY0, gridZ) * fracY0 + this.offset(seed, gridX1, gridY0, gridZ),
			offset111 = this.slopeX(seed, gridX1, gridY1, gridZ) * fracX1 + this.slopeY(seed, gridX1, gridY1, gridZ) * fracY1 + this.offset(seed, gridX1, gridY1, gridZ);
		for (int index = 0; true /* break in the middle of the loop. */;) {
			double fracZ0 = modZ * rcpZ;
			double fracZ1 = fracZ0 - 1.0D;
			samples.setD(
				index,
				Interpolator.mixSmoothUnchecked(
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeZ000 * fracZ0 + offset000,
							slopeZ010 * fracZ0 + offset010,
							smoothY
						),
						Interpolator.mixLinear(
							slopeZ100 * fracZ0 + offset100,
							slopeZ110 * fracZ0 + offset110,
							smoothY
						),
						smoothX
					),
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeZ001 * fracZ1 + offset001,
							slopeZ011 * fracZ1 + offset011,
							smoothY
						),
						Interpolator.mixLinear(
							slopeZ101 * fracZ1 + offset101,
							slopeZ111 * fracZ1 + offset111,
							smoothY
						),
						smoothX
					),
					fracZ0
				)
			);
			if (++index  >= sampleCount) break;
			if (++modZ   >= scaleZ) {
				modZ      = 0;
				slopeZ000 = slopeZ001;
				slopeZ010 = slopeZ011;
				slopeZ100 = slopeZ101;
				slopeZ110 = slopeZ111;
				offset000 = offset001;
				offset010 = offset011;
				offset100 = offset101;
				offset110 = offset111;
				gridZ    += scaleZ;
				slopeZ001 = this.slopeZ(seed, gridX0, gridY0, gridZ);
				slopeZ011 = this.slopeZ(seed, gridX0, gridY1, gridZ);
				slopeZ101 = this.slopeZ(seed, gridX1, gridY0, gridZ);
				slopeZ111 = this.slopeZ(seed, gridX1, gridY1, gridZ);
				offset001 = this.slopeX(seed, gridX0, gridY0, gridZ) * fracX0 + this.slopeY(seed, gridX0, gridY0, gridZ) * fracY0 + this.offset(seed, gridX0, gridY0, gridZ);
				offset011 = this.slopeX(seed, gridX0, gridY1, gridZ) * fracX0 + this.slopeY(seed, gridX0, gridY1, gridZ) * fracY1 + this.offset(seed, gridX0, gridY1, gridZ);
				offset101 = this.slopeX(seed, gridX1, gridY0, gridZ) * fracX1 + this.slopeY(seed, gridX1, gridY0, gridZ) * fracY0 + this.offset(seed, gridX1, gridY0, gridZ);
				offset111 = this.slopeX(seed, gridX1, gridY1, gridZ) * fracX1 + this.slopeY(seed, gridX1, gridY1, gridZ) * fracY1 + this.offset(seed, gridX1, gridY1, gridZ);
			}
		}
	}
}