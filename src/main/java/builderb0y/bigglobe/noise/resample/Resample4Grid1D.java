package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.polynomials.Polynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;

public abstract class Resample4Grid1D extends ResampleGrid1D {

	public Resample4Grid1D(Grid1D source, int scaleX) {
		super(source, scaleX);
	}

	@Override
	public double getValue(long seed, int x) {
		int scaleX = this.scaleX;
		int modX = BigGlobeMath.modulus_BP(x, scaleX);
		int gridX = x - modX;
		return this.polyForm().interpolate(
			this.source.getValue(seed, gridX - scaleX),
			this.source.getValue(seed, gridX),
			this.source.getValue(seed, gridX + scaleX),
			this.source.getValue(seed, gridX + (scaleX << 1)),
			this.rcpX,
			modX * this.rcpX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		Grid1D source = this.source;
		int scaleX = this.scaleX;
		int modX = BigGlobeMath.modulus_BP(startX, scaleX);
		int gridX = startX - modX;
		Polynomial polynomial = this.polyForm().createPolynomial(
			source.getValue(seed, gridX - scaleX),
			source.getValue(seed, gridX),
			source.getValue(seed, gridX += scaleX),
			source.getValue(seed, gridX += scaleX),
			this.rcpX
		);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, polynomial.interpolate(modX * this.rcpX));
			if (++index >= sampleCount) break;
			if (++modX >= scaleX) {
				modX = 0;
				polynomial.push(source.getValue(seed, gridX += scaleX), this.rcpX);
			}
		}
	}

	@Override
	public abstract PolyForm4 polyForm();
}