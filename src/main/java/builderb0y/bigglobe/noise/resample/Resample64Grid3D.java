package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.polynomials.Polynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial4.PolyForm4;

/** a ResampleGrid3D which internally interpolates between 8 sample points. */
public abstract class Resample64Grid3D extends ResampleGrid3D {

	public Resample64Grid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int scaleZ = this.scaleZ;
		int modX = BigGlobeMath.modulus_BP(x, scaleX);
		int modY = BigGlobeMath.modulus_BP(y, scaleY);
		int modZ = BigGlobeMath.modulus_BP(z, scaleZ);
		int gridX = x - modX;
		int gridY = y - modY;
		int gridZ = z - modZ;
		double fracX = modX * this.rcpX;
		double fracY = modY * this.rcpY;
		double fracZ = modZ * this.rcpZ;
		PolyForm4 formX = this.polyFormX();
		PolyForm4 formY = this.polyFormY();
		PolyForm4 formZ = this.polyFormZ();
		return formX.interpolate(
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX -= scaleX, gridY - scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX += scaleX, gridY - scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX += scaleX, gridY - scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
					this.rcpZ,fracZ
				),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX += scaleX, gridY - scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			this.rcpX,
			fracX
		);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int scaleZ = this.scaleZ;
		int modX = BigGlobeMath.modulus_BP(startX, scaleX);
		int modY = BigGlobeMath.modulus_BP(y,      scaleY);
		int modZ = BigGlobeMath.modulus_BP(z,      scaleZ);
		int gridX = startX - modX;
		int gridY = y      - modY;
		int gridZ = z      - modZ;
		double fracY = modY * this.rcpY;
		double fracZ = modZ * this.rcpZ;
		PolyForm4 formY = this.polyFormY();
		PolyForm4 formZ = this.polyFormZ();
		Polynomial polynomial = this.polyFormX().createPolynomial(
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX -= scaleX, gridY - scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX += scaleX, gridY - scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX += scaleX, gridY - scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX += scaleX, gridY - scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
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
						formZ.interpolate(
							this.source.getValue(seed, gridX += scaleX, gridY - scaleY, gridZ - scaleZ),
							this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
							this.source.getValue(seed, gridX, gridY - scaleY, gridZ + scaleZ),
							this.source.getValue(seed, gridX, gridY - scaleY, gridZ + (scaleZ << 1)),
							this.rcpZ,
							fracZ
						),
						formZ.interpolate(
							this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
							this.source.getValue(seed, gridX, gridY, gridZ),
							this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
							this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
							this.rcpZ,
							fracZ
						),
						formZ.interpolate(
							this.source.getValue(seed, gridX, gridY + scaleY, gridZ - scaleZ),
							this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
							this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
							this.source.getValue(seed, gridX, gridY + scaleY, gridZ + (scaleZ << 1)),
							this.rcpZ,
							fracZ
						),
						formZ.interpolate(
							this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ - scaleZ),
							this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
							this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + scaleZ),
							this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ + (scaleZ << 1)),
							this.rcpZ,
							fracZ
						),
						this.rcpY,
						fracY
					),
					this.rcpX
				);
			}
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int scaleZ = this.scaleZ;
		int modX = BigGlobeMath.modulus_BP(x,      scaleX);
		int modY = BigGlobeMath.modulus_BP(startY, scaleY);
		int modZ = BigGlobeMath.modulus_BP(z,      scaleZ);
		int gridX = x      - modX;
		int gridY = startY - modY;
		int gridZ = z      - modZ;
		double fracX = modX * this.rcpX;
		double fracZ = modZ * this.rcpZ;
		PolyForm4 formX = this.polyFormX();
		PolyForm4 formZ = this.polyFormZ();
		Polynomial polynomial = this.polyFormY().createPolynomial(
			formX.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX - scaleX, gridY -= scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX - scaleX, gridY += scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX - scaleX, gridY += scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX - scaleX, gridY += scaleY, gridZ - scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ - scaleZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + scaleZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + (scaleZ << 1)),
					this.rcpZ,
					fracZ
				),
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
						formZ.interpolate(
							this.source.getValue(seed, gridX - scaleX, gridY += scaleY, gridZ - scaleZ),
							this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
							this.source.getValue(seed, gridX - scaleX, gridY, gridZ + scaleZ),
							this.source.getValue(seed, gridX - scaleX, gridY, gridZ + (scaleZ << 1)),
							this.rcpZ,
							fracZ
						),
						formZ.interpolate(
							this.source.getValue(seed, gridX, gridY, gridZ - scaleZ),
							this.source.getValue(seed, gridX, gridY, gridZ),
							this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
							this.source.getValue(seed, gridX, gridY, gridZ + (scaleZ << 1)),
							this.rcpZ,
							fracZ
						),
						formZ.interpolate(
							this.source.getValue(seed, gridX + scaleX, gridY, gridZ - scaleZ),
							this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
							this.source.getValue(seed, gridX + scaleX, gridY, gridZ + scaleZ),
							this.source.getValue(seed, gridX + scaleX, gridY, gridZ + (scaleZ << 1)),
							this.rcpZ,
							fracZ
						),
						formZ.interpolate(
							this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ - scaleZ),
							this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
							this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + scaleZ),
							this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ + (scaleZ << 1)),
							this.rcpZ,
							fracZ
						),
						this.rcpX,
						fracX
					),
					this.rcpY
				);
			}
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int scaleZ = this.scaleZ;
		int modX = BigGlobeMath.modulus_BP(x,      scaleX);
		int modY = BigGlobeMath.modulus_BP(y,      scaleY);
		int modZ = BigGlobeMath.modulus_BP(startZ, scaleZ);
		int gridX = x      - modX;
		int gridY = y      - modY;
		int gridZ = startZ - modZ;
		double fracX = modX * this.rcpX;
		double fracY = modY * this.rcpY;
		PolyForm4 formX = this.polyFormX();
		PolyForm4 formY = this.polyFormY();
		Polynomial polynomial = this.polyFormZ().createPolynomial(
			formX.interpolate(
				formY.interpolate(
					this.source.getValue(seed, gridX - scaleX, gridY - scaleY, gridZ -= scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + (scaleX << 1), gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formY.interpolate(
					this.source.getValue(seed, gridX - scaleX, gridY - scaleY, gridZ += scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + (scaleX << 1), gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formY.interpolate(
					this.source.getValue(seed, gridX - scaleX, gridY - scaleY, gridZ += scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + (scaleX << 1), gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formY.interpolate(
					this.source.getValue(seed, gridX - scaleX, gridY - scaleY, gridZ += scaleZ),
					this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX - scaleX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + (scaleX << 1), gridY - scaleY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX + (scaleX << 1), gridY + (scaleY << 1), gridZ),
					this.rcpY,
					fracY
				),
				this.rcpX,
				fracX
			),
			this.rcpZ
		);
		for (int index = 0; true /* break in the middle of the loop */;) {
			samples.setD(index, polynomial.interpolate(modZ * this.rcpZ));
			if (++index >= sampleCount) break;
			if (++modZ >= scaleZ) {
				modZ = 0;
				polynomial.push(
					formX.interpolate(
						formY.interpolate(
							this.source.getValue(seed, gridX - scaleX, gridY - scaleY, gridZ += scaleZ),
							this.source.getValue(seed, gridX - scaleX, gridY, gridZ),
							this.source.getValue(seed, gridX - scaleX, gridY + scaleY, gridZ),
							this.source.getValue(seed, gridX - scaleX, gridY + (scaleY << 1), gridZ),
							this.rcpY,
							fracY
						),
						formY.interpolate(
							this.source.getValue(seed, gridX, gridY - scaleY, gridZ),
							this.source.getValue(seed, gridX, gridY, gridZ),
							this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
							this.source.getValue(seed, gridX, gridY + (scaleY << 1), gridZ),
							this.rcpY,
							fracY
						),
						formY.interpolate(
							this.source.getValue(seed, gridX + scaleX, gridY - scaleY, gridZ),
							this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
							this.source.getValue(seed, gridX + scaleX, gridY + scaleY, gridZ),
							this.source.getValue(seed, gridX + scaleX, gridY + (scaleY << 1), gridZ),
							this.rcpY,
							fracY
						),
						formY.interpolate(
							this.source.getValue(seed, gridX + (scaleX << 1), gridY - scaleY, gridZ),
							this.source.getValue(seed, gridX + (scaleX << 1), gridY, gridZ),
							this.source.getValue(seed, gridX + (scaleX << 1), gridY + scaleY, gridZ),
							this.source.getValue(seed, gridX + (scaleX << 1), gridY + (scaleY << 1), gridZ),
							this.rcpY,
							fracY
						),
						this.rcpX,
						fracX
					),
					this.rcpZ
				);
			}
		}
	}

	@Override
	public abstract PolyForm4 polyFormX();

	@Override
	public abstract PolyForm4 polyFormY();

	@Override
	public abstract PolyForm4 polyFormZ();
}