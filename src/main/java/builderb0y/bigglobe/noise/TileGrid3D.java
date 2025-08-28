package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;

public class TileGrid3D implements Grid3D {

	public final Grid3D source;
	public final @VerifyIntRange(min = 0L) @Alias("scale") int scaleX, scaleY, scaleZ;

	public TileGrid3D(Grid3D source, int scaleX, int scaleY, int scaleZ) {
		this.source = source;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.scaleZ = scaleZ;
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		if (this.scaleX > 0) {
			x = BigGlobeMath.modulus_BP(x, this.scaleX);
		}
		if (this.scaleY > 0) {
			y = BigGlobeMath.modulus_BP(y, this.scaleY);
		}
		if (this.scaleZ > 0) {
			z = BigGlobeMath.modulus_BP(z, this.scaleZ);
		}
		return this.source.getValue(seed, x, y, z);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		if (this.scaleY > 0) {
			y = BigGlobeMath.modulus_BP(y, this.scaleY);
		}
		if (this.scaleZ > 0) {
			z = BigGlobeMath.modulus_BP(z, this.scaleZ);
		}
		if (this.scaleX > 0) {
			int modX = BigGlobeMath.modulus_BP(startX, this.scaleX);
			int limit = Math.min(this.scaleX - modX, sampleCount);
			this.source.getBulkX(seed, modX, y, z, samples.prefix(limit));
			for (int soFar = limit; soFar < sampleCount; soFar += limit) {
				limit = Math.min(this.scaleX, sampleCount - soFar);
				this.source.getBulkX(seed, 0, y, z, samples.sliceOffsetLength(soFar, limit));
			}
		}
		else {
			this.source.getBulkX(seed, startX, y, z, samples);
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		if (this.scaleX > 0) {
			x = BigGlobeMath.modulus_BP(x, this.scaleX);
		}
		if (this.scaleZ > 0) {
			z = BigGlobeMath.modulus_BP(z, this.scaleZ);
		}
		if (this.scaleY > 0) {
			int modY = BigGlobeMath.modulus_BP(startY, this.scaleY);
			int limit = Math.min(this.scaleY - modY, sampleCount);
			this.source.getBulkY(seed, x, modY, z, samples.prefix(limit));
			for (int soFar = limit; soFar < sampleCount; soFar += limit) {
				limit = Math.min(this.scaleY, sampleCount - soFar);
				this.source.getBulkY(seed, x, 0, z, samples.sliceOffsetLength(soFar, limit));
			}
		}
		else {
			this.source.getBulkY(seed, x, startY, z, samples);
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		if (this.scaleX > 0) {
			x = BigGlobeMath.modulus_BP(x, this.scaleX);
		}
		if (this.scaleY > 0) {
			y = BigGlobeMath.modulus_BP(y, this.scaleY);
		}
		if (this.scaleZ > 0) {
			int modZ = BigGlobeMath.modulus_BP(startZ, this.scaleZ);
			int limit = Math.min(this.scaleZ - modZ, sampleCount);
			this.source.getBulkZ(seed, x, y, modZ, samples.prefix(limit));
			for (int soFar = limit; soFar < sampleCount; soFar += limit) {
				limit = Math.min(this.scaleZ, sampleCount - soFar);
				this.source.getBulkZ(seed, x, y, 0, samples.sliceOffsetLength(soFar, limit));
			}
		}
		else {
			this.source.getBulkZ(seed, x, y, startZ, samples);
		}
	}

	@Override
	public double minValue() {
		return this.source.minValue();
	}

	@Override
	public double maxValue() {
		return this.source.maxValue();
	}
}