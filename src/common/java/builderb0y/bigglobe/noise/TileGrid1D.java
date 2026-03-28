package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;

public class TileGrid1D implements Grid1D {

	public final Grid1D source;
	public final @VerifyIntRange(min = 0L)
	@Alias("scale") int scaleX;

	public TileGrid1D(Grid1D source, int scaleX) {
		this.source = source;
		this.scaleX = scaleX;
	}

	@Override
	public double getValue(long seed, int x) {
		if (this.scaleX > 0) {
			x = BigGlobeMath.modulus_BP(x, this.scaleX);
		}
		return this.source.getValue(seed, x);
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		int sampleCount = samples.length();
		if (sampleCount <= 0) return;
		if (this.scaleX > 0) {
			int modX = BigGlobeMath.modulus_BP(startX, this.scaleX);
			int limit = Math.min(this.scaleX - modX, sampleCount);
			this.source.getBulkX(seed, modX, samples.prefix(limit));
			for (int soFar = limit; soFar < sampleCount; soFar += limit) {
				limit = Math.min(this.scaleX, sampleCount - soFar);
				this.source.getBulkX(seed, 0, samples.sliceOffsetLength(soFar, limit));
			}
		}
		else {
			this.source.getBulkX(seed, startX, samples);
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