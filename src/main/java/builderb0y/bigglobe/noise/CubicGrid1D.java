package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.settings.Seed;

import static builderb0y.bigglobe.math.Interpolator.*;

public class CubicGrid1D extends AbstractGrid1D {

	public static final double MAX_OVERSHOOT = 5.0D / 4.0D; //mixCubic(-1.0D, 1.0D, 1.0D, -1.0D, 0.5D);

	public CubicGrid1D(Seed salt, double amplitude, int scaleX) {
		super(salt, amplitude, scaleX);
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
	public double getValue_X(long seed, int relativeX, double fracX) {
		return mixCubic(
			this.getValue_None(seed, relativeX - 1),
			this.getValue_None(seed, relativeX    ),
			this.getValue_None(seed, relativeX + 1),
			this.getValue_None(seed, relativeX + 2),
			fracX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, double[] samples, int sampleCount) {
		final int scaleX = this.scaleX;
		final double rcpX = this.rcpX;
		int relativeX = Math.floorDiv(startX, scaleX);
		int fracX = BigGlobeMath.modulus_BP(startX, scaleX);
		double a = this.getValue_None(seed,   relativeX - 1);
		double b = this.getValue_None(seed,   relativeX    );
		double c = this.getValue_None(seed, ++relativeX    );
		double d = this.getValue_None(seed, ++relativeX    );
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples[index] = fracX == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracX * rcpX);
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX = 0;
				a = b;
				b = c;
				c = d;
				d = this.getValue_None(seed, ++relativeX);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
		this.scale(samples, sampleCount);
	}

	@Override
	public double fracX(int fracX) {
		return fracX * this.rcpX;
	}
}