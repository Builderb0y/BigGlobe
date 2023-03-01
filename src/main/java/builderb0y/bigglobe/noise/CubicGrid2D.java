package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.settings.Seed;

import static builderb0y.bigglobe.math.Interpolator.*;

public class CubicGrid2D extends AbstractGrid2D {

	public static final double MAX_OVERSHOOT = 25.0D / 16.0D; //mixCubic(-CubicGrid1D.MAX_OVERSHOOT, CubicGrid1D.MAX_OVERSHOOT, CubicGrid1D.MAX_OVERSHOOT, -CubicGrid1D.MAX_OVERSHOOT, 0.5D);

	public CubicGrid2D(Seed salt, double amplitude, int scaleX, int scaleY) {
		super(salt, amplitude, scaleX, scaleY);
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
	public double getValue_XY(long seed, int relativeX, int relativeY, double fracX, double fracY) {
		return mixCubic(
			this.getValue_X(seed, relativeX, relativeY - 1, fracX),
			this.getValue_X(seed, relativeX, relativeY,     fracX),
			this.getValue_X(seed, relativeX, relativeY + 1, fracX),
			this.getValue_X(seed, relativeX, relativeY + 2, fracX),
			fracY
		);
	}

	@Override
	public double getValue_X(long seed, int relativeX, int relativeY, double fracX) {
		return mixCubic(
			this.getValue_None(seed, relativeX - 1, relativeY),
			this.getValue_None(seed, relativeX,     relativeY),
			this.getValue_None(seed, relativeX + 1, relativeY),
			this.getValue_None(seed, relativeX + 2, relativeY),
			fracX
		);
	}

	@Override
	public double getValue_Y(long seed, int relativeX, int relativeY, double fracY) {
		return mixCubic(
			this.getValue_None(seed, relativeX, relativeY - 1),
			this.getValue_None(seed, relativeX, relativeY    ),
			this.getValue_None(seed, relativeX, relativeY + 1),
			this.getValue_None(seed, relativeX, relativeY + 2),
			fracY
		);
	}

	@Override
	public void getValuesX_None(long seed, int absoluteX, int absoluteY, final double[] samples, final int sampleCount) {
		final int scaleX = this.scaleX;
		final double rcpX = this.rcpX;
		int relativeX = Math.floorDiv(absoluteX, scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int fracX = BigGlobeMath.modulus_BP(absoluteX, scaleX);
		double a = this.getValue_None(seed,   relativeX - 1, relativeY);
		double b = this.getValue_None(seed,   relativeX,     relativeY);
		double c = this.getValue_None(seed, ++relativeX,     relativeY);
		double d = this.getValue_None(seed, ++relativeX,     relativeY);
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
				d = this.getValue_None(seed, ++relativeX, relativeY);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesX_Y(long seed, int absoluteX, int absoluteY, double fracY, final double[] samples, final int sampleCount) {
		final int scaleX = this.scaleX;
		final double rcpX = this.rcpX;
		int relativeX = Math.floorDiv(absoluteX, scaleX);
		int relativeY = Math.floorDiv(absoluteY, this.scaleY);
		int fracX = BigGlobeMath.modulus_BP(absoluteX, scaleX);
		double a = this.getValue_Y(seed,   relativeX - 1, relativeY, fracY);
		double b = this.getValue_Y(seed,   relativeX,     relativeY, fracY);
		double c = this.getValue_Y(seed, ++relativeX,     relativeY, fracY);
		double d = this.getValue_Y(seed, ++relativeX,     relativeY, fracY);
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
				d = this.getValue_Y(seed, ++relativeX, relativeY, fracY);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesY_None(long seed, int absoluteX, int absoluteY, final double[] samples, final int sampleCount) {
		final int scaleY = this.scaleY;
		final double rcpY = this.rcpY;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, scaleY);
		int fracY = BigGlobeMath.modulus_BP(absoluteY, scaleY);
		double a = this.getValue_None(seed, relativeX,   relativeY - 1);
		double b = this.getValue_None(seed, relativeX,   relativeY    );
		double c = this.getValue_None(seed, relativeX, ++relativeY    );
		double d = this.getValue_None(seed, relativeX, ++relativeY    );
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
				d = this.getValue_None(seed, relativeX, ++relativeY);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}

	@Override
	public void getValuesY_X(long seed, int absoluteX, int absoluteY, double fracX, final double[] samples, final int sampleCount) {
		final int scaleY = this.scaleY;
		final double rcpY = this.rcpY;
		int relativeX = Math.floorDiv(absoluteX, this.scaleX);
		int relativeY = Math.floorDiv(absoluteY, scaleY);
		int fracY = BigGlobeMath.modulus_BP(absoluteY, scaleY);
		double a = this.getValue_X(seed, relativeX,   relativeY - 1, fracX);
		double b = this.getValue_X(seed, relativeX,   relativeY,     fracX);
		double c = this.getValue_X(seed, relativeX, ++relativeY,     fracX);
		double d = this.getValue_X(seed, relativeX, ++relativeY,     fracX);
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
				d = this.getValue_X(seed, relativeX, ++relativeY, fracX);
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
}