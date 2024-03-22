package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;

import static builderb0y.bigglobe.math.Interpolator.*;

public class CubicResampleGrid1D extends ResampleGrid1D {

	public static final double
		MIN_OVERSHOOT = -5.0D / 4.0D * 0.5D + 0.5D,
		MAX_OVERSHOOT = +5.0D / 4.0D * 0.5D + 0.5D;

	public final transient double minValue, maxValue;

	public CubicResampleGrid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
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
	public double getValue(long seed, int x) {
		int fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int gridX;
		return (
			fracX == 0
			? this.source.getValue(seed, x)
			: mixCubic(
				this.source.getValue(seed, (gridX = x - fracX) - this.scaleX),
				this.source.getValue(seed, gridX),
				this.source.getValue(seed, gridX += this.scaleX),
				this.source.getValue(seed, gridX + this.scaleX),
				fracX * this.rcpX
			)
		);
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		Grid1D source = this.source;
		int scaleX = this.scaleX;
		double rcpX = this.rcpX;
		int fracX = BigGlobeMath.modulus_BP(startX, scaleX);
		int gridX = startX - fracX;
		double a = source.getValue(seed, gridX - 1      );
		double b = source.getValue(seed, gridX          );
		double c = source.getValue(seed, gridX += scaleX);
		double d = source.getValue(seed, gridX += scaleX);
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? b : combineCubicTerms(term1, term2, term3, term4, fracX * rcpX));
			if (++index >= sampleCount) break;
			if (++fracX == scaleX) {
				fracX = 0;
				a = b;
				b = c;
				c = d;
				d = source.getValue(seed, gridX += scaleX);
				term1 = cubicTerm1(a, b, c, d);
				term2 = cubicTerm2(a, b, c, d);
				term3 = cubicTerm3(a, b, c, d);
				term4 = cubicTerm4(a, b, c, d);
			}
		}
	}
}