package builderb0y.bigglobe.noise;

public class OffsetGrid1D implements Grid1D {

	public final Grid1D source;
	public final int offsetX;

	public OffsetGrid1D(Grid1D source, int offsetX) {
		this.source  = source;
		this.offsetX = offsetX;
	}

	@Override
	public double getValue(long seed, int x) {
		return this.source.getValue(seed, x + this.offsetX);
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		this.source.getBulkX(seed, startX + this.offsetX, samples);
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