package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.settings.Seed;

import static builderb0y.bigglobe.math.Interpolator.*;

public class CubicGrid3D extends AbstractGrid3D {

	public static final double MAX_OVERSHOOT = 125.0D / 64.0D; //mixCubic(-CubicGrid2D.MAX_OVERSHOOT, CubicGrid2D.MAX_OVERSHOOT, CubicGrid2D.MAX_OVERSHOOT, -CubicGrid2D.MAX_OVERSHOOT, 0.5D);

	public CubicGrid3D(Seed salt, double amplitude, int scaleX, int scaleY, int scaleZ) {
		super(salt, amplitude, scaleX, scaleY, scaleZ);
	}

	@Override
	public double minValue() {
		return -MAX_OVERSHOOT * this.amplitude;
	}

	@Override
	public double maxValue() {
		return MAX_OVERSHOOT * this.amplitude;
	}

	@Override
	public double getValue_X(long seed, int relativeX, int relativeY, int relativeZ, double fracX) {
		return mixCubic(
			this.getValue_None(seed, relativeX - 1, relativeY, relativeZ),
			this.getValue_None(seed, relativeX,     relativeY, relativeZ),
			this.getValue_None(seed, relativeX + 1, relativeY, relativeZ),
			this.getValue_None(seed, relativeX + 2, relativeY, relativeZ),
			fracX
		);
	}

	@Override
	public double getValue_Y(long seed, int relativeX, int relativeY, int relativeZ, double fracY) {
		return mixCubic(
			this.getValue_None(seed, relativeX, relativeY - 1, relativeZ),
			this.getValue_None(seed, relativeX, relativeY,     relativeZ),
			this.getValue_None(seed, relativeX, relativeY + 1, relativeZ),
			this.getValue_None(seed, relativeX, relativeY + 2, relativeZ),
			fracY
		);
	}

	@Override
	public double getValue_Z(long seed, int relativeX, int relativeY, int relativeZ, double fracZ) {
		return mixCubic(
			this.getValue_None(seed, relativeX, relativeY, relativeZ - 1),
			this.getValue_None(seed, relativeX, relativeY, relativeZ    ),
			this.getValue_None(seed, relativeX, relativeY, relativeZ + 1),
			this.getValue_None(seed, relativeX, relativeY, relativeZ + 2),
			fracZ
		);
	}

	@Override
	public double getValue_XY(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracY) {
		return mixCubic(
			this.getValue_X(seed, relativeX, relativeY - 1, relativeZ, fracX),
			this.getValue_X(seed, relativeX, relativeY,     relativeZ, fracX),
			this.getValue_X(seed, relativeX, relativeY + 1, relativeZ, fracX),
			this.getValue_X(seed, relativeX, relativeY + 2, relativeZ, fracX),
			fracY
		);
	}

	@Override
	public double getValue_XZ(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracZ) {
		return mixCubic(
			this.getValue_X(seed, relativeX, relativeY, relativeZ - 1, fracX),
			this.getValue_X(seed, relativeX, relativeY, relativeZ,     fracX),
			this.getValue_X(seed, relativeX, relativeY, relativeZ + 1, fracX),
			this.getValue_X(seed, relativeX, relativeY, relativeZ + 2, fracX),
			fracZ
		);
	}

	@Override
	public double getValue_YZ(long seed, int relativeX, int relativeY, int relativeZ, double fracY, double fracZ) {
		return mixCubic(
			this.getValue_Y(seed, relativeX, relativeY, relativeZ - 1, fracY),
			this.getValue_Y(seed, relativeX, relativeY, relativeZ,     fracY),
			this.getValue_Y(seed, relativeX, relativeY, relativeZ + 1, fracY),
			this.getValue_Y(seed, relativeX, relativeY, relativeZ + 2, fracY),
			fracZ
		);
	}

	@Override
	public double getValue_XYZ(long seed, int relativeX, int relativeY, int relativeZ, double fracX, double fracY, double fracZ) {
		return mixCubic(
			this.getValue_XY(seed, relativeX, relativeY, relativeZ - 1, fracX, fracY),
			this.getValue_XY(seed, relativeX, relativeY, relativeZ,     fracX, fracY),
			this.getValue_XY(seed, relativeX, relativeY, relativeZ + 1, fracX, fracY),
			this.getValue_XY(seed, relativeX, relativeY, relativeZ + 2, fracX, fracY),
			fracZ
		);
	}

	@Override
	public void getValuesX_None(long seed, int absoluteX, int absoluteY, int absoluteZ, final double[] samples, final int sampleCount) {
		final int scaleX = this.scaleX;
		final double rcpX = this.rcpX;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, this.scaleZ);
		int fracX = BigGlobeMath.modulus_BP(absoluteX, scaleX);
		double a = this.getValue_None(seed,   relativeX - 1, relativeY, relativeZ) * amplitude;
		double b = this.getValue_None(seed,   relativeX,     relativeY, relativeZ) * amplitude;
		double c = this.getValue_None(seed, ++relativeX,     relativeY, relativeZ) * amplitude;
		double d = this.getValue_None(seed, ++relativeX,     relativeY, relativeZ) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracX == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracX * rcpX);
			if (++i >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_None(seed, ++relativeX, relativeY, relativeZ) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesX_Y(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracY, final double[] samples, final int sampleCount) {
		final int scaleX = this.scaleX;
		final double rcpX = this.rcpX;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, this.scaleZ);
		int fracX = BigGlobeMath.modulus_BP(absoluteX, scaleX);
		double a = this.getValue_Y(seed,   relativeX - 1, relativeY, relativeZ, fracY) * amplitude;
		double b = this.getValue_Y(seed,   relativeX,     relativeY, relativeZ, fracY) * amplitude;
		double c = this.getValue_Y(seed, ++relativeX,     relativeY, relativeZ, fracY) * amplitude;
		double d = this.getValue_Y(seed, ++relativeX,     relativeY, relativeZ, fracY) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracX == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracX * rcpX);
			if (++i >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_Y(seed, ++relativeX, relativeY, relativeZ, fracY) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesX_Z(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracZ, final double[] samples, final int sampleCount) {
		final int scaleX = this.scaleX;
		final double rcpX = this.rcpX;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, this.scaleZ);
		int fracX = BigGlobeMath.modulus_BP(absoluteX, scaleX);
		double a = this.getValue_Z(seed,   relativeX - 1, relativeY, relativeZ, fracZ) * amplitude;
		double b = this.getValue_Z(seed,   relativeX,     relativeY, relativeZ, fracZ) * amplitude;
		double c = this.getValue_Z(seed, ++relativeX,     relativeY, relativeZ, fracZ) * amplitude;
		double d = this.getValue_Z(seed, ++relativeX,     relativeY, relativeZ, fracZ) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracX == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracX * rcpX);
			if (++i >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_Z(seed, ++relativeX, relativeY, relativeZ, fracZ) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesX_YZ(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracY, double fracZ, final double[] samples, final int sampleCount) {
		final int scaleX = this.scaleX;
		final double rcpX = this.rcpX;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, this.scaleZ);
		int fracX = BigGlobeMath.modulus_BP(absoluteX, scaleX);
		double a = this.getValue_YZ(seed,   relativeX - 1, relativeY, relativeZ, fracY, fracZ) * amplitude;
		double b = this.getValue_YZ(seed,   relativeX,     relativeY, relativeZ, fracY, fracZ) * amplitude;
		double c = this.getValue_YZ(seed, ++relativeX,     relativeY, relativeZ, fracY, fracZ) * amplitude;
		double d = this.getValue_YZ(seed, ++relativeX,     relativeY, relativeZ, fracY, fracZ) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracX == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracX * rcpX);
			if (++i >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_YZ(seed, ++relativeX, relativeY, relativeZ, fracY, fracZ) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesY_None(long seed, int absoluteX, int absoluteY, int absoluteZ, final double[] samples, final int sampleCount) {
		final int scaleY = this.scaleY;
		final double rcpY = this.rcpY;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, this.scaleZ);
		int fracY = BigGlobeMath.modulus_BP(absoluteY, scaleY);
		double a = this.getValue_None(seed, relativeX,   relativeY - 1, relativeZ) * amplitude;
		double b = this.getValue_None(seed, relativeX,   relativeY,     relativeZ) * amplitude;
		double c = this.getValue_None(seed, relativeX, ++relativeY,     relativeZ) * amplitude;
		double d = this.getValue_None(seed, relativeX, ++relativeY,     relativeZ) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracY == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracY * rcpY);
			if (++i >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_None(seed, relativeX, ++relativeY, relativeZ) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesY_X(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracX, final double[] samples, final int sampleCount) {
		final int scaleY = this.scaleY;
		final double rcpY = this.rcpY;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, this.scaleZ);
		int fracY = BigGlobeMath.modulus_BP(absoluteY, scaleY);
		double a = this.getValue_X(seed, relativeX,   relativeY - 1, relativeZ, fracX) * amplitude;
		double b = this.getValue_X(seed, relativeX,   relativeY,     relativeZ, fracX) * amplitude;
		double c = this.getValue_X(seed, relativeX, ++relativeY,     relativeZ, fracX) * amplitude;
		double d = this.getValue_X(seed, relativeX, ++relativeY,     relativeZ, fracX) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracY == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracY * rcpY);
			if (++i >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_X(seed, relativeX, ++relativeY, relativeZ, fracX) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesY_Z(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracZ, final double[] samples, final int sampleCount) {
		final int scaleY = this.scaleY;
		final double rcpY = this.rcpY;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, this.scaleZ);
		int fracY = BigGlobeMath.modulus_BP(absoluteY, scaleY);
		double a = this.getValue_Z(seed, relativeX,   relativeY - 1, relativeZ, fracZ) * amplitude;
		double b = this.getValue_Z(seed, relativeX,   relativeY,     relativeZ, fracZ) * amplitude;
		double c = this.getValue_Z(seed, relativeX, ++relativeY,     relativeZ, fracZ) * amplitude;
		double d = this.getValue_Z(seed, relativeX, ++relativeY,     relativeZ, fracZ) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracY == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracY * rcpY);
			if (++i >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_Z(seed, relativeX, ++relativeY, relativeZ, fracZ) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesY_XZ(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracX, double fracZ, final double[] samples, final int sampleCount) {
		final int scaleY = this.scaleY;
		final double rcpY = this.rcpY;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, this.scaleZ);
		int fracY = BigGlobeMath.modulus_BP(absoluteY, scaleY);
		double a = this.getValue_XZ(seed, relativeX,   relativeY - 1, relativeZ, fracX, fracZ) * amplitude;
		double b = this.getValue_XZ(seed, relativeX,   relativeY,     relativeZ, fracX, fracZ) * amplitude;
		double c = this.getValue_XZ(seed, relativeX, ++relativeY,     relativeZ, fracX, fracZ) * amplitude;
		double d = this.getValue_XZ(seed, relativeX, ++relativeY,     relativeZ, fracX, fracZ) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracY == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracY * rcpY);
			if (++i >= sampleCount) break;
			if (++fracY == scaleY) {
				fracY = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_XZ(seed, relativeX, ++relativeY, relativeZ, fracX, fracZ) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesZ_None(long seed, int absoluteX, int absoluteY, int absoluteZ, final double[] samples, final int sampleCount) {
		final int scaleZ = this.scaleZ;
		final double rcpZ = this.rcpZ;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, scaleZ);
		int fracZ = BigGlobeMath.modulus_BP(absoluteZ, scaleZ);
		double a = this.getValue_None(seed, relativeX, relativeY,   relativeZ - 1) * amplitude;
		double b = this.getValue_None(seed, relativeX, relativeY,   relativeZ    ) * amplitude;
		double c = this.getValue_None(seed, relativeX, relativeY, ++relativeZ    ) * amplitude;
		double d = this.getValue_None(seed, relativeX, relativeY, ++relativeZ    ) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracZ == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracZ * rcpZ);
			if (++i >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_None(seed, relativeX, relativeY, ++relativeZ) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesZ_X(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracX, final double[] samples, final int sampleCount) {
		final int scaleZ = this.scaleZ;
		final double rcpZ = this.rcpZ;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, scaleZ);
		int fracZ = BigGlobeMath.modulus_BP(absoluteZ, scaleZ);
		double a = this.getValue_X(seed, relativeX, relativeY,   relativeZ - 1, fracX) * amplitude;
		double b = this.getValue_X(seed, relativeX, relativeY,   relativeZ,     fracX) * amplitude;
		double c = this.getValue_X(seed, relativeX, relativeY, ++relativeZ,     fracX) * amplitude;
		double d = this.getValue_X(seed, relativeX, relativeY, ++relativeZ,     fracX) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracZ == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracZ * rcpZ);
			if (++i >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_X(seed, relativeX, relativeY, ++relativeZ, fracX) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesZ_Y(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracY, final double[] samples, final int sampleCount) {
		final int scaleZ = this.scaleZ;
		final double rcpZ = this.rcpZ;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, scaleZ);
		int fracZ = BigGlobeMath.modulus_BP(absoluteZ, scaleZ);
		double a = this.getValue_Y(seed, relativeX, relativeY,   relativeZ - 1, fracY) * amplitude;
		double b = this.getValue_Y(seed, relativeX, relativeY,   relativeZ,     fracY) * amplitude;
		double c = this.getValue_Y(seed, relativeX, relativeY, ++relativeZ,     fracY) * amplitude;
		double d = this.getValue_Y(seed, relativeX, relativeY, ++relativeZ,     fracY) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracZ == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracZ * rcpZ);
			if (++i >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_Y(seed, relativeX, relativeY, ++relativeZ, fracY) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesZ_XY(long seed, int absoluteX, int absoluteY, int absoluteZ, double fracX, double fracY, final double[] samples, final int sampleCount) {
		final int scaleZ = this.scaleZ;
		final double rcpZ = this.rcpZ;
		final double amplitude = this.amplitude;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int relativeZ = Math.floorDiv(absoluteZ, scaleZ);
		int fracZ = BigGlobeMath.modulus_BP(absoluteZ, scaleZ);
		double a = this.getValue_XY(seed, relativeX, relativeY,   relativeZ - 1, fracX, fracY) * amplitude;
		double b = this.getValue_XY(seed, relativeX, relativeY,   relativeZ,     fracX, fracY) * amplitude;
		double c = this.getValue_XY(seed, relativeX, relativeY, ++relativeZ,     fracX, fracY) * amplitude;
		double d = this.getValue_XY(seed, relativeX, relativeY, ++relativeZ,     fracX, fracY) * amplitude;
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int i = 0; true /* break in the middle of the loop */;) {
			samples[i] = fracZ == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracZ * rcpZ);
			if (++i >= sampleCount) break;
			if (++fracZ == scaleZ) {
				fracZ = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_XY(seed, relativeX, relativeY, ++relativeZ, fracX, fracY) * amplitude;
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public double fracX(int fracX) {
		return fracX * this.rcpX;
	}

	@Override
	public double fracY(int fracY) {
		return fracY * this.rcpY;
	}

	@Override
	public double fracZ(int fracZ) {
		return fracZ * this.rcpZ;
	}
}