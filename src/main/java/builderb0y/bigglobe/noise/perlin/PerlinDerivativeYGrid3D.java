package builderb0y.bigglobe.noise.perlin;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.settings.Seed;

public class PerlinDerivativeYGrid3D extends PerlinBaseGrid3D {

	public PerlinDerivativeYGrid3D(Seed salt, int scaleX, int scaleY, int scaleZ, double max_slope, double max_offset) {
		super(salt, scaleX, scaleY, scaleZ, max_slope, max_offset);
	}

	@Override
	public double minValue() {
		throw new UnsupportedOperationException("I don't know how to find the local minimum of the derivative of perlin noise.");
	}

	@Override
	public double maxValue() {
		throw new UnsupportedOperationException("I don't know how to find the local maximum of the derivative of perlin noise.");
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		seed = this.salt.xor(seed);
		int
			modX = BigGlobeMath.modulus_BP(x, this.scaleX),
			modY = BigGlobeMath.modulus_BP(y, this.scaleY),
			modZ = BigGlobeMath.modulus_BP(z, this.scaleZ),
			gridX0 = x - modX,
			gridY0 = y - modY,
			gridZ0 = z - modZ,
			gridX1 = gridX0 + this.scaleX,
			gridY1 = gridY0 + this.scaleY,
			gridZ1 = gridZ0 + this.scaleZ;
		double
			fracX0 = modX * this.rcpX,
			fracY0 = modY * this.rcpY,
			fracZ0 = modZ * this.rcpZ,
			fracX1 = fracX0 - 1.0D,
			fracY1 = fracY0 - 1.0D,
			fracZ1 = fracZ0 - 1.0D,
			smoothX = Interpolator.smooth(fracX0),
			smoothZ = Interpolator.smooth(fracZ0),
			slopeX000 = this.slopeX(seed, gridX0, gridY0, gridZ0),
			slopeX001 = this.slopeX(seed, gridX0, gridY0, gridZ1),
			slopeX010 = this.slopeX(seed, gridX0, gridY1, gridZ0),
			slopeX011 = this.slopeX(seed, gridX0, gridY1, gridZ1),
			slopeX100 = this.slopeX(seed, gridX1, gridY0, gridZ0),
			slopeX101 = this.slopeX(seed, gridX1, gridY0, gridZ1),
			slopeX110 = this.slopeX(seed, gridX1, gridY1, gridZ0),
			slopeX111 = this.slopeX(seed, gridX1, gridY1, gridZ1),
			slopeY000 = this.slopeY(seed, gridX0, gridY0, gridZ0),
			slopeY001 = this.slopeY(seed, gridX0, gridY0, gridZ1),
			slopeY010 = this.slopeY(seed, gridX0, gridY1, gridZ0),
			slopeY011 = this.slopeY(seed, gridX0, gridY1, gridZ1),
			slopeY100 = this.slopeY(seed, gridX1, gridY0, gridZ0),
			slopeY101 = this.slopeY(seed, gridX1, gridY0, gridZ1),
			slopeY110 = this.slopeY(seed, gridX1, gridY1, gridZ0),
			slopeY111 = this.slopeY(seed, gridX1, gridY1, gridZ1),
			slopeZ000 = this.slopeZ(seed, gridX0, gridY0, gridZ0),
			slopeZ001 = this.slopeZ(seed, gridX0, gridY0, gridZ1),
			slopeZ010 = this.slopeZ(seed, gridX0, gridY1, gridZ0),
			slopeZ011 = this.slopeZ(seed, gridX0, gridY1, gridZ1),
			slopeZ100 = this.slopeZ(seed, gridX1, gridY0, gridZ0),
			slopeZ101 = this.slopeZ(seed, gridX1, gridY0, gridZ1),
			slopeZ110 = this.slopeZ(seed, gridX1, gridY1, gridZ0),
			slopeZ111 = this.slopeZ(seed, gridX1, gridY1, gridZ1),
			offset000 = this.offset(seed, gridX0, gridY0, gridZ0),
			offset001 = this.offset(seed, gridX0, gridY0, gridZ1),
			offset010 = this.offset(seed, gridX0, gridY1, gridZ0),
			offset011 = this.offset(seed, gridX0, gridY1, gridZ1),
			offset100 = this.offset(seed, gridX1, gridY0, gridZ0),
			offset101 = this.offset(seed, gridX1, gridY0, gridZ1),
			offset110 = this.offset(seed, gridX1, gridY1, gridZ0),
			offset111 = this.offset(seed, gridX1, gridY1, gridZ1);
		return Interpolator.dMixPerlin(
			Interpolator.mixLinear(
				Interpolator.mixLinear(
					slopeX000 * fracX0 + slopeY000 * fracY0 + slopeZ000 * fracZ0 + offset000,
					slopeX001 * fracX0 + slopeY001 * fracY0 + slopeZ001 * fracZ1 + offset001,
					smoothZ
				),
				Interpolator.mixLinear(
					slopeX100 * fracX1 + slopeY100 * fracY0 + slopeZ100 * fracZ0 + offset100,
					slopeX101 * fracX1 + slopeY101 * fracY0 + slopeZ101 * fracZ1 + offset101,
					smoothZ
				),
				smoothX
			),
			Interpolator.mixLinear(
				Interpolator.mixLinear(
					slopeX010 * fracX0 + slopeY010 * fracY1 + slopeZ010 * fracZ0 + offset010,
					slopeX011 * fracX0 + slopeY011 * fracY1 + slopeZ011 * fracZ1 + offset011,
					smoothZ
				),
				Interpolator.mixLinear(
					slopeX110 * fracX1 + slopeY110 * fracY1 + slopeZ110 * fracZ0 + offset110,
					slopeX111 * fracX1 + slopeY111 * fracY1 + slopeZ111 * fracZ1 + offset111,
					smoothZ
				),
				smoothX
			),
			Interpolator.mixLinear(
				Interpolator.mixLinear(slopeY000, slopeY001, smoothZ),
				Interpolator.mixLinear(slopeY100, slopeY101, smoothZ),
				smoothX
			),
			Interpolator.mixLinear(
				Interpolator.mixLinear(slopeY010, slopeY011, smoothZ),
				Interpolator.mixLinear(slopeY110, slopeY111, smoothZ),
				smoothX
			),
			fracY0
		) * this.rcpY;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX = this.scaleX,
			scaleY = this.scaleY,
			scaleZ = this.scaleZ,
			modX = BigGlobeMath.modulus_BP(startX, scaleX),
			modY = BigGlobeMath.modulus_BP(y, scaleY),
			modZ = BigGlobeMath.modulus_BP(z, scaleZ),
			gridX = startX - modX,
			gridY0 = y - modY,
			gridZ0 = z - modZ,
			gridY1 = gridY0 + scaleY,
			gridZ1 = gridZ0 + scaleZ;
		double
			rcpX = this.rcpX,
			rcpY = this.rcpY,
			fracY0 = modY * rcpY,
			fracZ0 = modZ * this.rcpZ,
			fracY1 = fracY0 - 1.0D,
			fracZ1 = fracZ0 - 1.0D,
			smoothY = Interpolator.smooth(fracY0),
			smoothZ = Interpolator.smooth(fracZ0),
			dSmoothY = Interpolator.smoothDerivative(fracY0),
			slopeX000 = this.slopeX(seed, gridX, gridY0, gridZ0),
			slopeX001 = this.slopeX(seed, gridX, gridY0, gridZ1),
			slopeX010 = this.slopeX(seed, gridX, gridY1, gridZ0),
			slopeX011 = this.slopeX(seed, gridX, gridY1, gridZ1),
			slopeY000 = this.slopeY(seed, gridX, gridY0, gridZ0),
			slopeY001 = this.slopeY(seed, gridX, gridY0, gridZ1),
			slopeY010 = this.slopeY(seed, gridX, gridY1, gridZ0),
			slopeY011 = this.slopeY(seed, gridX, gridY1, gridZ1),
			slopeZ000 = this.slopeZ(seed, gridX, gridY0, gridZ0),
			slopeZ001 = this.slopeZ(seed, gridX, gridY0, gridZ1),
			slopeZ010 = this.slopeZ(seed, gridX, gridY1, gridZ0),
			slopeZ011 = this.slopeZ(seed, gridX, gridY1, gridZ1),
			offset000 = this.offset(seed, gridX, gridY0, gridZ0),
			offset001 = this.offset(seed, gridX, gridY0, gridZ1),
			offset010 = this.offset(seed, gridX, gridY1, gridZ0),
			offset011 = this.offset(seed, gridX, gridY1, gridZ1),
			slopeX100 = this.slopeX(seed, gridX += scaleX, gridY0, gridZ0),
			slopeX101 = this.slopeX(seed, gridX, gridY0, gridZ1),
			slopeX110 = this.slopeX(seed, gridX, gridY1, gridZ0),
			slopeX111 = this.slopeX(seed, gridX, gridY1, gridZ1),
			slopeY100 = this.slopeY(seed, gridX, gridY0, gridZ0),
			slopeY101 = this.slopeY(seed, gridX, gridY0, gridZ1),
			slopeY110 = this.slopeY(seed, gridX, gridY1, gridZ0),
			slopeY111 = this.slopeY(seed, gridX, gridY1, gridZ1),
			slopeZ100 = this.slopeZ(seed, gridX, gridY0, gridZ0),
			slopeZ101 = this.slopeZ(seed, gridX, gridY0, gridZ1),
			slopeZ110 = this.slopeZ(seed, gridX, gridY1, gridZ0),
			slopeZ111 = this.slopeZ(seed, gridX, gridY1, gridZ1),
			offset100 = this.offset(seed, gridX, gridY0, gridZ0),
			offset101 = this.offset(seed, gridX, gridY0, gridZ1),
			offset110 = this.offset(seed, gridX, gridY1, gridZ0),
			offset111 = this.offset(seed, gridX, gridY1, gridZ1);
		for (int index = 0; true /* break in the middle of the loop. */; ) {
			double fracX0 = modX * rcpX;
			double fracX1 = fracX0 - 1.0D;
			double smoothX = Interpolator.smooth(fracX0);
			samples.setD(
				index,
				Interpolator.dMixPerlinExplicit(
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeX000 * fracX0 + slopeY000 * fracY0 + slopeZ000 * fracZ0 + offset000,
							slopeX001 * fracX0 + slopeY001 * fracY0 + slopeZ001 * fracZ1 + offset001,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeX100 * fracX1 + slopeY100 * fracY0 + slopeZ100 * fracZ0 + offset100,
							slopeX101 * fracX1 + slopeY101 * fracY0 + slopeZ101 * fracZ1 + offset101,
							smoothZ
						),
						smoothX
					),
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeX010 * fracX0 + slopeY010 * fracY1 + slopeZ010 * fracZ0 + offset010,
							slopeX011 * fracX0 + slopeY011 * fracY1 + slopeZ011 * fracZ1 + offset011,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeX110 * fracX1 + slopeY110 * fracY1 + slopeZ110 * fracZ0 + offset110,
							slopeX111 * fracX1 + slopeY111 * fracY1 + slopeZ111 * fracZ1 + offset111,
							smoothZ
						),
						smoothX
					),
					smoothY,
					Interpolator.mixLinear(
						Interpolator.mixLinear(slopeY000, slopeY001, smoothZ),
						Interpolator.mixLinear(slopeY100, slopeY101, smoothZ),
						smoothX
					),
					Interpolator.mixLinear(
						Interpolator.mixLinear(slopeY010, slopeY011, smoothZ),
						Interpolator.mixLinear(slopeY110, slopeY111, smoothZ),
						smoothX
					),
					dSmoothY
				) * rcpY
			);
			if (++index >= sampleCount) break;
			if (++modX >= scaleX) {
				modX = 0;
				slopeX000 = slopeX100;
				slopeX001 = slopeX101;
				slopeX010 = slopeX110;
				slopeX011 = slopeX111;
				slopeY000 = slopeY100;
				slopeY001 = slopeY101;
				slopeY010 = slopeY110;
				slopeY011 = slopeY111;
				slopeZ000 = slopeZ100;
				slopeZ001 = slopeZ101;
				slopeZ010 = slopeZ110;
				slopeZ011 = slopeZ111;
				offset000 = offset100;
				offset001 = offset101;
				offset010 = offset110;
				offset011 = offset111;
				gridX += scaleX;
				slopeX100 = this.slopeX(seed, gridX, gridY0, gridZ0);
				slopeX101 = this.slopeX(seed, gridX, gridY0, gridZ1);
				slopeX110 = this.slopeX(seed, gridX, gridY1, gridZ0);
				slopeX111 = this.slopeX(seed, gridX, gridY1, gridZ1);
				slopeY100 = this.slopeY(seed, gridX, gridY0, gridZ0);
				slopeY101 = this.slopeY(seed, gridX, gridY0, gridZ1);
				slopeY110 = this.slopeY(seed, gridX, gridY1, gridZ0);
				slopeY111 = this.slopeY(seed, gridX, gridY1, gridZ1);
				slopeZ100 = this.slopeZ(seed, gridX, gridY0, gridZ0);
				slopeZ101 = this.slopeZ(seed, gridX, gridY0, gridZ1);
				slopeZ110 = this.slopeZ(seed, gridX, gridY1, gridZ0);
				slopeZ111 = this.slopeZ(seed, gridX, gridY1, gridZ1);
				offset100 = this.offset(seed, gridX, gridY0, gridZ0);
				offset101 = this.offset(seed, gridX, gridY0, gridZ1);
				offset110 = this.offset(seed, gridX, gridY1, gridZ0);
				offset111 = this.offset(seed, gridX, gridY1, gridZ1);
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX = this.scaleX,
			scaleY = this.scaleY,
			scaleZ = this.scaleZ,
			modX = BigGlobeMath.modulus_BP(x, scaleX),
			modY = BigGlobeMath.modulus_BP(startY, scaleY),
			modZ = BigGlobeMath.modulus_BP(z, scaleZ),
			gridX0 = x - modX,
			gridY = startY - modY,
			gridZ0 = z - modZ,
			gridX1 = gridX0 + scaleX,
			gridZ1 = gridZ0 + scaleZ;
		double
			rcpY = this.rcpY,
			fracX0 = modX * this.rcpX,
			fracZ0 = modZ * this.rcpZ,
			fracX1 = fracX0 - 1.0D,
			fracZ1 = fracZ0 - 1.0D,
			smoothX = Interpolator.smooth(fracX0),
			smoothZ = Interpolator.smooth(fracZ0),
			slopeX000 = this.slopeX(seed, gridX0, gridY, gridZ0),
			slopeX001 = this.slopeX(seed, gridX0, gridY, gridZ1),
			slopeX100 = this.slopeX(seed, gridX1, gridY, gridZ0),
			slopeX101 = this.slopeX(seed, gridX1, gridY, gridZ1),
			slopeY000 = this.slopeY(seed, gridX0, gridY, gridZ0),
			slopeY001 = this.slopeY(seed, gridX0, gridY, gridZ1),
			slopeY100 = this.slopeY(seed, gridX1, gridY, gridZ0),
			slopeY101 = this.slopeY(seed, gridX1, gridY, gridZ1),
			slopeZ000 = this.slopeZ(seed, gridX0, gridY, gridZ0),
			slopeZ001 = this.slopeZ(seed, gridX0, gridY, gridZ1),
			slopeZ100 = this.slopeZ(seed, gridX1, gridY, gridZ0),
			slopeZ101 = this.slopeZ(seed, gridX1, gridY, gridZ1),
			offset000 = this.offset(seed, gridX0, gridY, gridZ0),
			offset001 = this.offset(seed, gridX0, gridY, gridZ1),
			offset100 = this.offset(seed, gridX1, gridY, gridZ0),
			offset101 = this.offset(seed, gridX1, gridY, gridZ1),
			slopeX010 = this.slopeX(seed, gridX0, gridY += scaleY, gridZ0),
			slopeX011 = this.slopeX(seed, gridX0, gridY, gridZ1),
			slopeX110 = this.slopeX(seed, gridX1, gridY, gridZ0),
			slopeX111 = this.slopeX(seed, gridX1, gridY, gridZ1),
			slopeY010 = this.slopeY(seed, gridX0, gridY, gridZ0),
			slopeY011 = this.slopeY(seed, gridX0, gridY, gridZ1),
			slopeY110 = this.slopeY(seed, gridX1, gridY, gridZ0),
			slopeY111 = this.slopeY(seed, gridX1, gridY, gridZ1),
			slopeZ010 = this.slopeZ(seed, gridX0, gridY, gridZ0),
			slopeZ011 = this.slopeZ(seed, gridX0, gridY, gridZ1),
			slopeZ110 = this.slopeZ(seed, gridX1, gridY, gridZ0),
			slopeZ111 = this.slopeZ(seed, gridX1, gridY, gridZ1),
			offset010 = this.offset(seed, gridX0, gridY, gridZ0),
			offset011 = this.offset(seed, gridX0, gridY, gridZ1),
			offset110 = this.offset(seed, gridX1, gridY, gridZ0),
			offset111 = this.offset(seed, gridX1, gridY, gridZ1),
			partial0 = Interpolator.mixLinear(
				Interpolator.mixLinear(slopeY000, slopeY001, smoothZ),
				Interpolator.mixLinear(slopeY100, slopeY101, smoothZ),
				smoothX
			),
			partial1 = Interpolator.mixLinear(
				Interpolator.mixLinear(slopeY010, slopeY011, smoothZ),
				Interpolator.mixLinear(slopeY110, slopeY111, smoothZ),
				smoothX
			);
		for (int index = 0; true /* break in the middle of the loop. */; ) {
			double fracY0 = modY * rcpY;
			double fracY1 = fracY0 - 1.0D;
			samples.setD(
				index,
				Interpolator.dMixPerlin(
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeX000 * fracX0 + slopeY000 * fracY0 + slopeZ000 * fracZ0 + offset000,
							slopeX001 * fracX0 + slopeY001 * fracY0 + slopeZ001 * fracZ1 + offset001,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeX100 * fracX1 + slopeY100 * fracY0 + slopeZ100 * fracZ0 + offset100,
							slopeX101 * fracX1 + slopeY101 * fracY0 + slopeZ101 * fracZ1 + offset101,
							smoothZ
						),
						smoothX
					),
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeX010 * fracX0 + slopeY010 * fracY1 + slopeZ010 * fracZ0 + offset010,
							slopeX011 * fracX0 + slopeY011 * fracY1 + slopeZ011 * fracZ1 + offset011,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeX110 * fracX1 + slopeY110 * fracY1 + slopeZ110 * fracZ0 + offset110,
							slopeX111 * fracX1 + slopeY111 * fracY1 + slopeZ111 * fracZ1 + offset111,
							smoothZ
						),
						smoothX
					),
					partial0,
					partial1,
					fracY0
				) * rcpY
			);
			if (++index >= sampleCount) break;
			if (++modY >= scaleY) {
				modY = 0;
				slopeX000 = slopeX010;
				slopeX001 = slopeX011;
				slopeX100 = slopeX110;
				slopeX101 = slopeX111;
				slopeY000 = slopeY010;
				slopeY001 = slopeY011;
				slopeY100 = slopeY110;
				slopeY101 = slopeY111;
				slopeZ000 = slopeZ010;
				slopeZ001 = slopeZ011;
				slopeZ100 = slopeZ110;
				slopeZ101 = slopeZ111;
				offset000 = offset010;
				offset001 = offset011;
				offset100 = offset110;
				offset101 = offset111;
				gridY += scaleY;
				slopeX010 = this.slopeX(seed, gridX0, gridY, gridZ0);
				slopeX011 = this.slopeX(seed, gridX0, gridY, gridZ1);
				slopeX110 = this.slopeX(seed, gridX1, gridY, gridZ0);
				slopeX111 = this.slopeX(seed, gridX1, gridY, gridZ1);
				slopeY010 = this.slopeY(seed, gridX0, gridY, gridZ0);
				slopeY011 = this.slopeY(seed, gridX0, gridY, gridZ1);
				slopeY110 = this.slopeY(seed, gridX1, gridY, gridZ0);
				slopeY111 = this.slopeY(seed, gridX1, gridY, gridZ1);
				slopeZ010 = this.slopeZ(seed, gridX0, gridY, gridZ0);
				slopeZ011 = this.slopeZ(seed, gridX0, gridY, gridZ1);
				slopeZ110 = this.slopeZ(seed, gridX1, gridY, gridZ0);
				slopeZ111 = this.slopeZ(seed, gridX1, gridY, gridZ1);
				offset010 = this.offset(seed, gridX0, gridY, gridZ0);
				offset011 = this.offset(seed, gridX0, gridY, gridZ1);
				offset110 = this.offset(seed, gridX1, gridY, gridZ0);
				offset111 = this.offset(seed, gridX1, gridY, gridZ1);
				partial0 = Interpolator.mixLinear(
					Interpolator.mixLinear(slopeY000, slopeY001, smoothZ),
					Interpolator.mixLinear(slopeY100, slopeY101, smoothZ),
					smoothX
				);
				partial1 = Interpolator.mixLinear(
					Interpolator.mixLinear(slopeY010, slopeY011, smoothZ),
					Interpolator.mixLinear(slopeY110, slopeY111, smoothZ),
					smoothX
				);
			}
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX = this.scaleX,
			scaleY = this.scaleY,
			scaleZ = this.scaleZ,
			modX = BigGlobeMath.modulus_BP(x, scaleX),
			modY = BigGlobeMath.modulus_BP(y, scaleY),
			modZ = BigGlobeMath.modulus_BP(startZ, scaleZ),
			gridX0 = x - modX,
			gridY0 = y - modY,
			gridZ = startZ - modZ,
			gridX1 = gridX0 + scaleX,
			gridY1 = gridY0 + scaleY;
		double
			rcpY = this.rcpY,
			rcpZ = this.rcpZ,
			fracX0 = modX * this.rcpX,
			fracY0 = modY * rcpY,
			fracX1 = fracX0 - 1.0D,
			fracY1 = fracY0 - 1.0D,
			smoothX = Interpolator.smooth(fracX0),
			smoothY = Interpolator.smooth(fracY0),
			dSmoothY = Interpolator.smoothDerivative(fracY0),
			slopeX000 = this.slopeX(seed, gridX0, gridY0, gridZ),
			slopeX010 = this.slopeX(seed, gridX0, gridY1, gridZ),
			slopeX100 = this.slopeX(seed, gridX1, gridY0, gridZ),
			slopeX110 = this.slopeX(seed, gridX1, gridY1, gridZ),
			slopeY000 = this.slopeY(seed, gridX0, gridY0, gridZ),
			slopeY010 = this.slopeY(seed, gridX0, gridY1, gridZ),
			slopeY100 = this.slopeY(seed, gridX1, gridY0, gridZ),
			slopeY110 = this.slopeY(seed, gridX1, gridY1, gridZ),
			slopeZ000 = this.slopeZ(seed, gridX0, gridY0, gridZ),
			slopeZ010 = this.slopeZ(seed, gridX0, gridY1, gridZ),
			slopeZ100 = this.slopeZ(seed, gridX1, gridY0, gridZ),
			slopeZ110 = this.slopeZ(seed, gridX1, gridY1, gridZ),
			offset000 = this.offset(seed, gridX0, gridY0, gridZ),
			offset010 = this.offset(seed, gridX0, gridY1, gridZ),
			offset100 = this.offset(seed, gridX1, gridY0, gridZ),
			offset110 = this.offset(seed, gridX1, gridY1, gridZ),
			slopeX001 = this.slopeX(seed, gridX0, gridY0, gridZ += scaleZ),
			slopeX011 = this.slopeX(seed, gridX0, gridY1, gridZ),
			slopeX101 = this.slopeX(seed, gridX1, gridY0, gridZ),
			slopeX111 = this.slopeX(seed, gridX1, gridY1, gridZ),
			slopeY001 = this.slopeY(seed, gridX0, gridY0, gridZ),
			slopeY011 = this.slopeY(seed, gridX0, gridY1, gridZ),
			slopeY101 = this.slopeY(seed, gridX1, gridY0, gridZ),
			slopeY111 = this.slopeY(seed, gridX1, gridY1, gridZ),
			slopeZ001 = this.slopeZ(seed, gridX0, gridY0, gridZ),
			slopeZ011 = this.slopeZ(seed, gridX0, gridY1, gridZ),
			slopeZ101 = this.slopeZ(seed, gridX1, gridY0, gridZ),
			slopeZ111 = this.slopeZ(seed, gridX1, gridY1, gridZ),
			offset001 = this.offset(seed, gridX0, gridY0, gridZ),
			offset011 = this.offset(seed, gridX0, gridY1, gridZ),
			offset101 = this.offset(seed, gridX1, gridY0, gridZ),
			offset111 = this.offset(seed, gridX1, gridY1, gridZ);
		for (int index = 0; true /* break in the middle of the loop. */; ) {
			double fracZ0 = modZ * rcpZ;
			double fracZ1 = fracZ0 - 1.0D;
			double smoothZ = Interpolator.smooth(fracZ0);
			samples.setD(
				index,
				Interpolator.dMixPerlinExplicit(
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeX000 * fracX0 + slopeY000 * fracY0 + slopeZ000 * fracZ0 + offset000,
							slopeX001 * fracX0 + slopeY001 * fracY0 + slopeZ001 * fracZ1 + offset001,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeX100 * fracX1 + slopeY100 * fracY0 + slopeZ100 * fracZ0 + offset100,
							slopeX101 * fracX1 + slopeY101 * fracY0 + slopeZ101 * fracZ1 + offset101,
							smoothZ
						),
						smoothX
					),
					Interpolator.mixLinear(
						Interpolator.mixLinear(
							slopeX010 * fracX0 + slopeY010 * fracY1 + slopeZ010 * fracZ0 + offset010,
							slopeX011 * fracX0 + slopeY011 * fracY1 + slopeZ011 * fracZ1 + offset011,
							smoothZ
						),
						Interpolator.mixLinear(
							slopeX110 * fracX1 + slopeY110 * fracY1 + slopeZ110 * fracZ0 + offset110,
							slopeX111 * fracX1 + slopeY111 * fracY1 + slopeZ111 * fracZ1 + offset111,
							smoothZ
						),
						smoothX
					),
					smoothY,
					Interpolator.mixLinear(
						Interpolator.mixLinear(slopeY000, slopeY001, smoothZ),
						Interpolator.mixLinear(slopeY100, slopeY101, smoothZ),
						smoothX
					),
					Interpolator.mixLinear(
						Interpolator.mixLinear(slopeY010, slopeY011, smoothZ),
						Interpolator.mixLinear(slopeY110, slopeY111, smoothZ),
						smoothX
					),
					dSmoothY
				) * rcpY
			);
			if (++index >= sampleCount) break;
			if (++modZ >= scaleZ) {
				modZ = 0;
				slopeX000 = slopeX001;
				slopeX010 = slopeX011;
				slopeX100 = slopeX101;
				slopeX110 = slopeX111;
				slopeY000 = slopeY001;
				slopeY010 = slopeY011;
				slopeY100 = slopeY101;
				slopeY110 = slopeY111;
				slopeZ000 = slopeZ001;
				slopeZ010 = slopeZ011;
				slopeZ100 = slopeZ101;
				slopeZ110 = slopeZ111;
				offset000 = offset001;
				offset010 = offset011;
				offset100 = offset101;
				offset110 = offset111;
				gridZ += scaleZ;
				slopeX001 = this.slopeX(seed, gridX0, gridY0, gridZ);
				slopeX011 = this.slopeX(seed, gridX0, gridY1, gridZ);
				slopeX101 = this.slopeX(seed, gridX1, gridY0, gridZ);
				slopeX111 = this.slopeX(seed, gridX1, gridY1, gridZ);
				slopeY001 = this.slopeY(seed, gridX0, gridY0, gridZ);
				slopeY011 = this.slopeY(seed, gridX0, gridY1, gridZ);
				slopeY101 = this.slopeY(seed, gridX1, gridY0, gridZ);
				slopeY111 = this.slopeY(seed, gridX1, gridY1, gridZ);
				slopeZ001 = this.slopeZ(seed, gridX0, gridY0, gridZ);
				slopeZ011 = this.slopeZ(seed, gridX0, gridY1, gridZ);
				slopeZ101 = this.slopeZ(seed, gridX1, gridY0, gridZ);
				slopeZ111 = this.slopeZ(seed, gridX1, gridY1, gridZ);
				offset001 = this.offset(seed, gridX0, gridY0, gridZ);
				offset011 = this.offset(seed, gridX0, gridY1, gridZ);
				offset101 = this.offset(seed, gridX1, gridY0, gridZ);
				offset111 = this.offset(seed, gridX1, gridY1, gridZ);
			}
		}
	}
}