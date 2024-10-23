package builderb0y.bigglobe.noise;

public class OffsetGrid2D implements Grid2D {

	public final Grid2D source;
	public final int offsetX, offsetY;

	public OffsetGrid2D(Grid2D source, int offsetX, int offsetY) {
		this.source  = source;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	@Override
	public double getValue(long seed, int x, int y) {
		return this.source.getValue(seed, x + this.offsetX, y + this.offsetY);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		this.source.getBulkX(seed, startX + this.offsetX, y + this.offsetY, samples);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		this.source.getBulkY(seed, x + this.offsetX, startY + this.offsetY, samples);
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