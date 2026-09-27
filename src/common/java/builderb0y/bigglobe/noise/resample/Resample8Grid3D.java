package builderb0y.bigglobe.noise.resample;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.polynomials.Polynomial;
import builderb0y.bigglobe.noise.polynomials.Polynomial2.PolyForm2;
import builderb0y.bigglobe.noise.polynomials.SmoothPolynomial;
import builderb0y.bigglobe.noise.polynomials.VectorizedSmoothPolynomial;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid3D;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

/**
a ResampleGrid3D which internally interpolates between 8 sample points.
*/
public abstract class Resample8Grid3D extends ResampleGrid3D {

	/**
	neighboring columns usually interpolate between the same 4 vertical lines of lattice points,
	so for sources which are cheap and only depend on their seed and position (white noise),
	the values along those lines are remembered and re-used by the next column.
	only used on {@link BigGlobeThreadPool} threads, which is where almost all noise is computed.
	other pools (minecraft's worker pool in particular) can start and stop lots of threads,
	and each one keeping its own cache would add up.
	*/
	public final transient ThreadLocal<LatticeCache> latticeCache;

	public Resample8Grid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		super(source, scaleX, scaleY, scaleZ);
		this.latticeCache = source instanceof WhiteNoiseGrid3D ? ThreadLocal.withInitial(LatticeCache::new) : null;
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		int modX = BigGlobeMath.modulus_BP(x, this.scaleX);
		int modY = BigGlobeMath.modulus_BP(y, this.scaleY);
		int modZ = BigGlobeMath.modulus_BP(z, this.scaleZ);
		int gridX = x - modX;
		int gridY = y - modY;
		int gridZ = z - modZ;
		double fracX = modX * this.rcpX;
		double fracY = modY * this.rcpY;
		double fracZ = modZ * this.rcpZ;
		PolyForm2 formX = this.polyFormX();
		PolyForm2 formY = this.polyFormY();
		PolyForm2 formZ = this.polyFormZ();
		return formX.interpolate(
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + this.scaleZ),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + this.scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + this.scaleY, gridZ + this.scaleZ),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX + this.scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + this.scaleX, gridY, gridZ + this.scaleZ),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + this.scaleX, gridY + this.scaleY, gridZ),
					this.source.getValue(seed, gridX + this.scaleX, gridY + this.scaleY, gridZ + this.scaleZ),
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
		int modY = BigGlobeMath.modulus_BP(y, scaleY);
		int modZ = BigGlobeMath.modulus_BP(z, scaleZ);
		int gridX = startX - modX;
		int gridY = y - modY;
		int gridZ = z - modZ;
		double fracY = modY * this.rcpY;
		double fracZ = modZ * this.rcpZ;
		PolyForm2 formY = this.polyFormY();
		PolyForm2 formZ = this.polyFormZ();
		Polynomial polynomial = this.polyFormX().createPolynomial(
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			formY.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX += scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
					this.rcpZ,
					fracZ
				),
				this.rcpY,
				fracY
			),
			this.rcpX
		);
		for (int index = 0; true /* break in the middle of the loop */; ) {
			samples.setD(index, polynomial.interpolate(modX * this.rcpX));
			if (++index >= sampleCount) break;
			if (++modX >= scaleX) {
				modX = 0;
				polynomial.push(
					formY.interpolate(
						formZ.interpolate(
							this.source.getValue(seed, gridX += scaleX, gridY, gridZ),
							this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
							this.rcpZ,
							fracZ
						),
						formZ.interpolate(
							this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
							this.source.getValue(seed, gridX, gridY + scaleY, gridZ + scaleZ),
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
		if (this.latticeCache != null && Thread.currentThread() instanceof BigGlobeThreadPool.WorkerThread) {
			this.getBulkYCached(seed, x, startY, z, samples);
		}
		else {
			this.getBulkYDirect(seed, x, startY, z, samples);
		}
	}

	/** computes every lattice point it needs directly from {@link #source}. */
	public void getBulkYDirect(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int scaleZ = this.scaleZ;
		int modX = BigGlobeMath.modulus_BP(x, scaleX);
		int modY = BigGlobeMath.modulus_BP(startY, scaleY);
		int modZ = BigGlobeMath.modulus_BP(z, scaleZ);
		int gridX = x - modX;
		int gridY = startY - modY;
		int gridZ = z - modZ;
		double fracX = modX * this.rcpX;
		double fracZ = modZ * this.rcpZ;
		PolyForm2 formX = this.polyFormX();
		PolyForm2 formZ = this.polyFormZ();
		Polynomial polynomial = this.polyFormY().createPolynomial(
			formX.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + scaleZ),
					this.rcpZ,
					fracZ
				),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formZ.interpolate(
					this.source.getValue(seed, gridX, gridY += scaleY, gridZ),
					this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
					this.rcpZ,
					fracZ
				),
				formZ.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ + scaleZ),
					this.rcpZ,
					fracZ
				),
				this.rcpX,
				fracX
			),
			this.rcpY
		);
		for (int index = 0; true /* break in the middle of the loop */; ) {
			samples.setD(index, polynomial.interpolate(modY * this.rcpY));
			if (++index >= sampleCount) break;
			if (++modY >= scaleY) {
				modY = 0;
				polynomial.push(
					formX.interpolate(
						formZ.interpolate(
							this.source.getValue(seed, gridX, gridY += scaleY, gridZ),
							this.source.getValue(seed, gridX, gridY, gridZ + scaleZ),
							this.rcpZ,
							fracZ
						),
						formZ.interpolate(
							this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
							this.source.getValue(seed, gridX + scaleX, gridY, gridZ + scaleZ),
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

	/** same as {@link #getBulkY}, but gets the lattice points from {@link #latticeCache}. */
	public void getBulkYCached(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int scaleZ = this.scaleZ;
		int modX = BigGlobeMath.modulus_BP(x, scaleX);
		int modY = BigGlobeMath.modulus_BP(startY, scaleY);
		int modZ = BigGlobeMath.modulus_BP(z, scaleZ);
		int gridX = x - modX;
		int gridY = startY - modY;
		int gridZ = z - modZ;
		double fracX = modX * this.rcpX;
		double fracZ = modZ * this.rcpZ;
		PolyForm2 formX = this.polyFormX();
		PolyForm2 formZ = this.polyFormZ();
		int rows = (modY + sampleCount - 1) / scaleY + 2;
		LatticeCache cache = this.latticeCache.get();
		LatticeLine line00 = cache.get(this.source, seed, gridX,          gridZ,          gridY, scaleX, scaleY, scaleZ, rows);
		LatticeLine line01 = cache.get(this.source, seed, gridX,          gridZ + scaleZ, gridY, scaleX, scaleY, scaleZ, rows);
		LatticeLine line10 = cache.get(this.source, seed, gridX + scaleX, gridZ,          gridY, scaleX, scaleY, scaleZ, rows);
		LatticeLine line11 = cache.get(this.source, seed, gridX + scaleX, gridZ + scaleZ, gridY, scaleX, scaleY, scaleZ, rows);
		double[] values00 = line00.values, values01 = line01.values, values10 = line10.values, values11 = line11.values;
		int row00 = (gridY - line00.minY) / scaleY;
		int row01 = (gridY - line01.minY) / scaleY;
		int row10 = (gridY - line10.minY) / scaleY;
		int row11 = (gridY - line11.minY) / scaleY;
		Polynomial polynomial = this.polyFormY().createPolynomial(
			formX.interpolate(
				formZ.interpolate(values00[row00], values01[row01], this.rcpZ, fracZ),
				formZ.interpolate(values10[row10], values11[row11], this.rcpZ, fracZ),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formZ.interpolate(values00[++row00], values01[++row01], this.rcpZ, fracZ),
				formZ.interpolate(values10[++row10], values11[++row11], this.rcpZ, fracZ),
				this.rcpX,
				fracX
			),
			this.rcpY
		);
		double[] values = cache.values(sampleCount);
		boolean vectorized = VectorizedSmoothPolynomial.AVAILABLE && polynomial instanceof SmoothPolynomial;
		for (int index = 0; true /* break in the middle of the loop */; ) {
			int count = Math.min(scaleY - modY, sampleCount - index);
			if (vectorized) {
				VectorizedSmoothPolynomial.Impl.interpolate((SmoothPolynomial)(polynomial), modY, this.rcpY, values, index, count);
			}
			else {
				for (int offset = 0; offset < count; offset++) {
					values[index + offset] = polynomial.interpolate((modY + offset) * this.rcpY);
				}
			}
			if ((index += count) >= sampleCount) break;
			modY = 0;
			polynomial.push(
				formX.interpolate(
					formZ.interpolate(values00[++row00], values01[++row01], this.rcpZ, fracZ),
					formZ.interpolate(values10[++row10], values11[++row11], this.rcpZ, fracZ),
					this.rcpX,
					fracX
				),
				this.rcpY
			);
		}
		samples.setAllD(values, sampleCount);
	}

	/** the values of a grid along one vertical line of lattice points. */
	public static class LatticeLine {

		public long seed;
		public int x, z, minY, rows;
		public double[] values;

		public boolean contains(long seed, int x, int z, int minY, int scaleY, int rows) {
			return (
				this.values != null &&
				this.x == x &&
				this.z == z &&
				this.seed == seed &&
				minY >= this.minY &&
				(minY - this.minY) / scaleY + rows <= this.rows
			);
		}

		public void compute(Grid3D source, long seed, int x, int z, int minY, int scaleY, int rows) {
			if (this.values != null && this.x == x && this.z == z && this.seed == seed) {
				//same line, but a different range of Y levels. cover both.
				int maxY = Math.max(minY + (rows - 1) * scaleY, this.minY + (this.rows - 1) * scaleY);
				minY = Math.min(minY, this.minY);
				rows = (maxY - minY) / scaleY + 1;
			}
			this.seed = seed;
			this.x = x;
			this.z = z;
			this.minY = minY;
			this.rows = rows;
			double[] values = this.values;
			if (values == null || values.length < rows) {
				this.values = values = new double[rows];
			}
			for (int row = 0, y = minY; row < rows; row++, y += scaleY) {
				values[row] = source.getValue(seed, x, y, z);
			}
		}
	}

	public static class LatticeCache {

		public static final int SLOTS = 64;

		public final LatticeLine[] lines = new LatticeLine[SLOTS];
		public double[] values = new double[0];

		public LatticeCache() {
			for (int slot = 0; slot < SLOTS; slot++) {
				this.lines[slot] = new LatticeLine();
			}
		}

		/** returns an array which can hold at least the given number of samples. */
		public double[] values(int count) {
			double[] values = this.values;
			if (values.length < count) this.values = values = new double[count];
			return values;
		}

		/**
		the 4 lines used by one column always have different parities,
		so they always end up in different quarters of the cache,
		and getting one of them can't overwrite another.
		*/
		public LatticeLine get(Grid3D source, long seed, int x, int z, int minY, int scaleX, int scaleY, int scaleZ, int rows) {
			int quarter = (((x / scaleX) & 1) << 1) | ((z / scaleZ) & 1);
			LatticeLine line = this.lines[(quarter << 4) | ((x * 31 + z) * 0x9E3779B9 >>> 28)];
			if (!line.contains(seed, x, z, minY, scaleY, rows)) {
				line.compute(source, seed, x, z, minY, scaleY, rows);
			}
			return line;
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		int scaleX = this.scaleX;
		int scaleY = this.scaleY;
		int scaleZ = this.scaleZ;
		int modX = BigGlobeMath.modulus_BP(x, scaleX);
		int modY = BigGlobeMath.modulus_BP(y, scaleY);
		int modZ = BigGlobeMath.modulus_BP(startZ, scaleZ);
		int gridX = x - modX;
		int gridY = y - modY;
		int gridZ = startZ - modZ;
		double fracX = modX * this.rcpX;
		double fracY = modY * this.rcpY;
		PolyForm2 formX = this.polyFormX();
		PolyForm2 formY = this.polyFormY();
		Polynomial polynomial = this.polyFormZ().createPolynomial(
			formX.interpolate(
				formY.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + scaleY, gridZ),
					this.rcpY,
					fracY
				),
				this.rcpX,
				fracX
			),
			formX.interpolate(
				formY.interpolate(
					this.source.getValue(seed, gridX, gridY, gridZ += scaleZ),
					this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
					this.rcpY,
					fracY
				),
				formY.interpolate(
					this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
					this.source.getValue(seed, gridX + scaleX, gridY + scaleY, gridZ),
					this.rcpY,
					fracY
				),
				this.rcpX,
				fracX
			),
			this.rcpZ
		);
		for (int index = 0; true /* break in the middle of the loop */; ) {
			samples.setD(index, polynomial.interpolate(modZ * this.rcpZ));
			if (++index >= sampleCount) break;
			if (++modZ >= scaleZ) {
				modZ = 0;
				polynomial.push(
					formX.interpolate(
						formY.interpolate(
							this.source.getValue(seed, gridX, gridY, gridZ += scaleZ),
							this.source.getValue(seed, gridX, gridY + scaleY, gridZ),
							this.rcpY,
							fracY
						),
						formY.interpolate(
							this.source.getValue(seed, gridX + scaleX, gridY, gridZ),
							this.source.getValue(seed, gridX + scaleX, gridY + scaleY, gridZ),
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
	public abstract PolyForm2 polyFormX();

	@Override
	public abstract PolyForm2 polyFormY();

	@Override
	public abstract PolyForm2 polyFormZ();
}