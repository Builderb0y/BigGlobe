package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.BigGlobeMath;

/** a ResampleGrid2D which internally interpolates between 4 sample points. */
public abstract class Resample4Grid2D extends ResampleGrid2D {

	public Resample4Grid2D(Grid2D source, int scaleX, int scaleY) {
		super(source, scaleX, scaleY);
	}

	@Override
	public double getValue(long seed, int x, int y) {
		int fracX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int fracY = BigGlobeMath.modulus_BP(y, this.scaleY);
		int gridX = x - fracX;
		int gridY = y - fracY;
		double scaledX = fracX * this.rcpX;
		double scaledY = fracY * this.rcpY;
		return this.interpolateX(
			this.interpolateY(
				this.source.getValue(seed, gridX, gridY),
				this.source.getValue(seed, gridX, gridY + this.scaleY),
				scaledY
			),
			this.interpolateY(
				this.source.getValue(seed, gridX + this.scaleX, gridY),
				this.source.getValue(seed, gridX + this.scaleX, gridY + this.scaleY),
				scaledY
			),
			scaledX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int fracX = BigGlobeMath.modulus_BP(startX, scaleX);
		int fracY = BigGlobeMath.modulus_BP(y,      scaleY);
		int gridX = startX - fracX;
		int gridY = y      - fracY;
		double scaledY = gridY * this.rcpY;
		Polynomial polynomial = this.xPolynomial(
			this.interpolateY(
				this.source.getValue(seed, gridX, gridY),
				this.source.getValue(seed, gridX, gridY + scaleY),
				scaledY
			),
			this.interpolateY(
				this.source.getValue(seed, gridX += scaleX, gridY),
				this.source.getValue(seed, gridX, gridY + scaleY),
				scaledY
			)
		);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, polynomial.interpolate(fracX * this.rcpX));
			if (++index >= sampleCount) break;
			if (++fracX >= scaleX) {
				fracX = 0;
				polynomial.push(
					this.interpolateY(
						this.source.getValue(seed, gridX += scaleX, gridY),
						this.source.getValue(seed, gridX, gridY + scaleY),
						scaledY
					)
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
		int fracX = BigGlobeMath.modulus_BP(x,      scaleX);
		int fracY = BigGlobeMath.modulus_BP(startY, scaleY);
		int gridX = x      - fracX;
		int gridY = startY - fracY;
		double scaledX = fracX * this.rcpX;
		Polynomial polynomial = this.yPolynomial(
			this.interpolateX(
				this.source.getValue(seed, gridX, gridY),
				this.source.getValue(seed, gridX + scaleX, gridY),
				scaledX
			),
			this.interpolateX(
				this.source.getValue(seed, gridX, gridY += scaleY),
				this.source.getValue(seed, gridX + scaleX, gridY),
				scaledX
			)
		);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, polynomial.interpolate(fracY * this.rcpY));
			if (++index >= sampleCount) break;
			if (++fracY >= scaleY) {
				fracY = 0;
				polynomial.push(
					this.interpolateX(
						this.source.getValue(seed, gridX, gridY += scaleY),
						this.source.getValue(seed, gridX + scaleX, gridY),
						scaledX
					)
				);
			}
		}
	}

	public abstract Polynomial xPolynomial(double value0, double value1);

	public abstract Polynomial yPolynomial(double value0, double value1);

	public abstract double interpolateX(double value0, double value1, double fraction);

	public abstract double interpolateY(double value0, double value1, double fraction);
}