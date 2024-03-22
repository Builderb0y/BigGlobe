package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;

import static builderb0y.bigglobe.math.Interpolator.mixLinear;

/** a ResampleGrid1D which internally interpolates between 2 sample points. */
public abstract class Resample2Grid1D extends ResampleGrid1D {

	public Resample2Grid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public double getValue(long seed, int x) {
		int fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		return (
			fracX == 0
			? this.source.getValue(seed, x)
			: this.doInterpolateX(seed, x - fracX, this.curveX(fracX))
		);
	}

	public double doInterpolateX(long seed, int x, double fracX) {
		return mixLinear(
			this.source.getValue(seed, x),
			this.source.getValue(seed, x + this.scaleX),
			fracX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX    = this.scaleX;
		Grid1D source = this.source;
		int fracX     = BigGlobeMath.modulus_BP(startX, scaleX);
		int gridX     = startX - fracX;
		double value0 = source.getValue(seed, gridX);
		double value1 = source.getValue(seed, gridX += scaleX);
		double diff   = value1 - value0;
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, fracX == 0 ? value0 : this.curveX(fracX) * diff + value0);
			if (++index >= sampleCount) break;
			if (++fracX >= scaleX) {
				fracX  = 0;
				value0 = value1;
				value1 = source.getValue(seed, gridX += scaleX);
				diff   = value1 - value0;
			}
		}
	}

	public abstract double curveX(int fracX);
}