package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.polynomials.Polynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;

/** a ResampleGrid2D which internally interpolates between 4 sample points. */
public abstract class Resample4Grid2D extends ResampleGrid2D {

	public Resample4Grid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public double getValue(long seed, int x, int y) {
		int modX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int modY = BigGlobeMath.modulus_BP(y, this.scaleY);
		int gridX = x - modX;
		int gridY = y - modY;
		double fracX = modX * this.rcpX;
		double fracY = modY * this.rcpY;
		PolyForm2 formY = this.polyFormY();
		return this.polyFormX().interpolate(
			formY.interpolate(
				this.source.getValue(seed, gridX, gridY),
				this.source.getValue(seed, gridX, gridY + this.scaleY),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				this.source.getValue(seed, gridX + this.scaleX, gridY),
				this.source.getValue(seed, gridX + this.scaleX, gridY + this.scaleY),
				this.rcpY,
				fracY
			),
			this.rcpX,
			fracX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int modX = BigGlobeMath.modulus_BP(startX, scaleX);
		int modY = BigGlobeMath.modulus_BP(y,      scaleY);
		int gridX = startX - modX;
		int gridY = y      - modY;
		double fracY = modY * this.rcpY;
		PolyForm2 formY = this.polyFormY();
		Polynomial polynomial = this.polyFormX().createPolynomial(
			formY.interpolate(
				this.source.getValue(seed, gridX, gridY),
				this.source.getValue(seed, gridX, gridY + scaleY),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				this.source.getValue(seed, gridX += scaleX, gridY),
				this.source.getValue(seed, gridX, gridY + scaleY),
				this.rcpY,
				fracY
			),
			this.rcpX
		);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, polynomial.interpolate(modX * this.rcpX));
			if (++index >= sampleCount) break;
			if (++modX >= scaleX) {
				modX = 0;
				polynomial.push(
					formY.interpolate(
						this.source.getValue(seed, gridX += scaleX, gridY),
						this.source.getValue(seed, gridX, gridY + scaleY),
						this.rcpY,
						fracY
					),
					this.rcpX
				);
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int modX = BigGlobeMath.modulus_BP(x,      scaleX);
		int modY = BigGlobeMath.modulus_BP(startY, scaleY);
		int gridX = x      - modX;
		int gridY = startY - modY;
		double fracX = modX * this.rcpX;
		PolyForm2 formX = this.polyFormX();
		Polynomial polynomial = this.polyFormY().createPolynomial(
			formX.interpolate(
				this.source.getValue(seed, gridX, gridY),
				this.source.getValue(seed, gridX + scaleX, gridY),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				this.source.getValue(seed, gridX, gridY += scaleY),
				this.source.getValue(seed, gridX + scaleX, gridY),
				this.rcpX,
				fracX
			),
			this.rcpY
		);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, polynomial.interpolate(modY * this.rcpY));
			if (++index >= sampleCount) break;
			if (++modY >= scaleY) {
				modY = 0;
				polynomial.push(
					formX.interpolate(
						this.source.getValue(seed, gridX, gridY += scaleY),
						this.source.getValue(seed, gridX + scaleX, gridY),
						this.rcpX,
						fracX
					),
					this.rcpY
				);
			}
		}
	}

	@Override
	public abstract PolyForm2 polyFormX();

	@Override
	public abstract PolyForm2 polyFormY();
}