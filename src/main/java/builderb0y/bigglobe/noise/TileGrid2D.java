package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;

public class TileGrid2D implements Grid2D {

	public final Grid2D source;
	public final @VerifyIntRange(min = 0L) @Alias("scale") int scaleX, scaleY;

	public TileGrid2D(Grid2D source, int scaleX, int scaleY) {
		this.source = source;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}

	@Override
	public double getValue(long seed, int x, int y) {
		if (this.scaleX > 0) {
			x = BigGlobeMath.modulus_BP(x, this.scaleX);
		}
		if (this.scaleY > 0) {
			y = BigGlobeMath.modulus_BP(y, this.scaleY);
		}
		return this.source.getValue(seed, x, y);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		if (this.scaleY > 0) {
			y = BigGlobeMath.modulus_BP(y, this.scaleY);
		}
		if (this.scaleX > 0) {
			int modX = BigGlobeMath.modulus_BP(startX, this.scaleX);
			int limit = Math.min(this.scaleX - modX, sampleCount);
			this.source.getBulkX(seed, modX, y, samples.prefix(limit));
			for (int soFar = limit; soFar < sampleCount; soFar += limit) {
				limit = Math.min(this.scaleX, sampleCount - soFar);
				this.source.getBulkX(seed, 0, y, samples.sliceOffsetLength(soFar, limit));
			}
		}
		else {
			this.source.getBulkX(seed, startX, y, samples);
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		if (this.scaleX > 0) {
			x = BigGlobeMath.modulus_BP(x, this.scaleX);
		}
		if (this.scaleY > 0) {
			int modY = BigGlobeMath.modulus_BP(startY, this.scaleY);
			int limit = Math.min(this.scaleY - modY, sampleCount);
			this.source.getBulkY(seed, x, modY, samples.prefix(limit));
			for (int soFar = limit; soFar < sampleCount; soFar += limit) {
				limit = Math.min(this.scaleY, sampleCount - soFar);
				this.source.getBulkY(seed, x, 0, samples.sliceOffsetLength(soFar, limit));
			}
		}
		else {
			this.source.getBulkY(seed, x, startY, samples);
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