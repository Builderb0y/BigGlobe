package builderb0y.bigglobe.noise.perlin;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.settings.Seed;

public class PerlinDerivativeXGrid2D extends PerlinBaseGrid2D {

	public PerlinDerivativeXGrid2D(Seed salt, int scaleX, int scaleY, double max_slope, double max_offset) {
		super(salt, scaleX, scaleY, max_slope, max_offset);
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
	public double getValue(long seed, int x, int y) {
		seed = this.salt.xor(seed);
		int
			modX = BigGlobeMath.modulus_BP(x, this.scaleX),
			modY = BigGlobeMath.modulus_BP(y, this.scaleY),
			gridX0 = x - modX,
			gridY0 = y - modY,
			gridX1 = gridX0 + this.scaleX,
			gridY1 = gridY0 + this.scaleY;
		double
			fracX0 = modX * this.rcpX,
			fracY0 = modY * this.rcpY,
			fracX1 = fracX0 - 1.0D,
			fracY1 = fracY0 - 1.0D,
			smoothY = Interpolator.smooth(fracY0),
			slopeX00 = this.slopeX(seed, gridX0, gridY0),
			slopeX01 = this.slopeX(seed, gridX0, gridY1),
			slopeX10 = this.slopeX(seed, gridX1, gridY0),
			slopeX11 = this.slopeX(seed, gridX1, gridY1),
			slopeY00 = this.slopeY(seed, gridX0, gridY0),
			slopeY01 = this.slopeY(seed, gridX0, gridY1),
			slopeY10 = this.slopeY(seed, gridX1, gridY0),
			slopeY11 = this.slopeY(seed, gridX1, gridY1),
			offset00 = this.offset(seed, gridX0, gridY0),
			offset01 = this.offset(seed, gridX0, gridY1),
			offset10 = this.offset(seed, gridX1, gridY0),
			offset11 = this.offset(seed, gridX1, gridY1);
		return Interpolator.dMixPerlin(
			Interpolator.mixLinear(
				slopeX00 * fracX0 + slopeY00 * fracY0 + offset00,
				slopeX01 * fracX0 + slopeY01 * fracY1 + offset01,
				smoothY
			),
			Interpolator.mixLinear(
				slopeX10 * fracX1 + slopeY10 * fracY0 + offset10,
				slopeX11 * fracX1 + slopeY11 * fracY1 + offset11,
				smoothY
			),
			Interpolator.mixLinear(
				slopeX00,
				slopeX01,
				smoothY
			),
			Interpolator.mixLinear(
				slopeX10,
				slopeX11,
				smoothY
			),
			fracX0
		) * this.rcpX;
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleX = this.scaleX,
			modX = BigGlobeMath.modulus_BP(startX, scaleX),
			modY = BigGlobeMath.modulus_BP(y, this.scaleY),
			gridX = startX - modX,
			gridY0 = y - modY,
			gridY1 = gridY0 + this.scaleY;
		double
			rcpX = this.rcpX,
			fracY0 = modY * this.rcpY,
			fracY1 = fracY0 - 1.0D,
			smoothY = Interpolator.smooth(fracY0),
			slopeX00 = this.slopeX(seed, gridX, gridY0),
			slopeX01 = this.slopeX(seed, gridX, gridY1),
			slopeY00 = this.slopeY(seed, gridX, gridY0),
			slopeY01 = this.slopeY(seed, gridX, gridY1),
			offset00 = this.offset(seed, gridX, gridY0),
			offset01 = this.offset(seed, gridX, gridY1),
			slopeX10 = this.slopeX(seed, gridX += scaleX, gridY0),
			slopeX11 = this.slopeX(seed, gridX, gridY1),
			slopeY10 = this.slopeY(seed, gridX, gridY0),
			slopeY11 = this.slopeY(seed, gridX, gridY1),
			offset10 = this.offset(seed, gridX, gridY0),
			offset11 = this.offset(seed, gridX, gridY1),
			partial0 = Interpolator.mixLinear(slopeX00, slopeX01, smoothY),
			partial1 = Interpolator.mixLinear(slopeX10, slopeX11, smoothY);
		for (int index = 0; true /* break in the middle of the loop. */; ) {
			double fracX0 = modX * rcpX;
			double fracX1 = fracX0 - 1.0D;
			samples.setD(
				index,
				Interpolator.dMixPerlin(
					Interpolator.mixLinear(
						slopeX00 * fracX0 + slopeY00 * fracY0 + offset00,
						slopeX01 * fracX0 + slopeY01 * fracY1 + offset01,
						smoothY
					),
					Interpolator.mixLinear(
						slopeX10 * fracX1 + slopeY10 * fracY0 + offset10,
						slopeX11 * fracX1 + slopeY11 * fracY1 + offset11,
						smoothY
					),
					partial0,
					partial1,
					fracX0
				) * rcpX
			);
			if (++index >= sampleCount) break;
			if (++modX >= scaleX) {
				modX = 0;
				slopeX00 = slopeX10;
				slopeX01 = slopeX11;
				slopeY00 = slopeY10;
				slopeY01 = slopeY11;
				offset00 = offset10;
				offset01 = offset11;
				gridX += scaleX;
				slopeX10 = this.slopeX(seed, gridX, gridY0);
				slopeX11 = this.slopeX(seed, gridX, gridY1);
				slopeY10 = this.slopeY(seed, gridX, gridY0);
				slopeY11 = this.slopeY(seed, gridX, gridY1);
				offset10 = this.offset(seed, gridX, gridY0);
				offset11 = this.offset(seed, gridX, gridY1);
				partial0 = Interpolator.mixLinear(slopeX00, slopeX01, smoothY);
				partial1 = Interpolator.mixLinear(slopeX10, slopeX11, smoothY);
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		seed = this.salt.xor(seed);
		int
			scaleY = this.scaleY,
			modX = BigGlobeMath.modulus_BP(x, this.scaleX),
			modY = BigGlobeMath.modulus_BP(startY, scaleY),
			gridX0 = x - modX,
			gridX1 = gridX0 + this.scaleX,
			gridY = startY - modY;
		double
			rcpX = this.rcpX,
			rcpY = this.rcpY,
			fracX0 = modX * rcpX,
			fracX1 = fracX0 - 1.0D,
			smoothX = Interpolator.smooth(fracX0),
			dSmoothX = Interpolator.smoothDerivative(fracX0),
			slopeX00 = this.slopeX(seed, gridX0, gridY),
			slopeX10 = this.slopeX(seed, gridX1, gridY),
			slopeY00 = this.slopeY(seed, gridX0, gridY),
			slopeY10 = this.slopeY(seed, gridX1, gridY),
			offset00 = this.offset(seed, gridX0, gridY),
			offset10 = this.offset(seed, gridX1, gridY),
			slopeX01 = this.slopeX(seed, gridX0, gridY += scaleY),
			slopeX11 = this.slopeX(seed, gridX1, gridY),
			slopeY01 = this.slopeY(seed, gridX0, gridY),
			slopeY11 = this.slopeY(seed, gridX1, gridY),
			offset01 = this.offset(seed, gridX0, gridY),
			offset11 = this.offset(seed, gridX1, gridY);
		for (int index = 0; true /* break in the middle of the loop. */; ) {
			double fracY0 = modY * rcpY;
			double fracY1 = fracY0 - 1.0D;
			double smoothY = Interpolator.smooth(fracY0);
			samples.setD(
				index,
				Interpolator.dMixPerlinExplicit(
					Interpolator.mixLinear(
						slopeX00 * fracX0 + slopeY00 * fracY0 + offset00,
						slopeX01 * fracX0 + slopeY01 * fracY1 + offset01,
						smoothY
					),
					Interpolator.mixLinear(
						slopeX10 * fracX1 + slopeY10 * fracY0 + offset10,
						slopeX11 * fracX1 + slopeY11 * fracY1 + offset11,
						smoothY
					),
					smoothX,
					Interpolator.mixLinear(
						slopeX00,
						slopeX01,
						smoothY
					),
					Interpolator.mixLinear(
						slopeX10,
						slopeX11,
						smoothY
					),
					dSmoothX
				) * rcpX
			);
			if (++index >= sampleCount) break;
			if (++modY >= scaleY) {
				modY = 0;
				slopeX00 = slopeX01;
				slopeX10 = slopeX11;
				slopeY00 = slopeY01;
				slopeY10 = slopeY11;
				offset00 = offset01;
				offset10 = offset11;
				gridY += scaleY;
				slopeX01 = this.slopeX(seed, gridX0, gridY);
				slopeX11 = this.slopeX(seed, gridX1, gridY);
				slopeY01 = this.slopeY(seed, gridX0, gridY);
				slopeY11 = this.slopeY(seed, gridX1, gridY);
				offset01 = this.offset(seed, gridX0, gridY);
				offset11 = this.offset(seed, gridX1, gridY);
			}
		}
	}
}