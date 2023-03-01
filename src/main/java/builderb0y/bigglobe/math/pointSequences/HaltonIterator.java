package builderb0y.bigglobe.math.pointSequences;

//https://en.wikipedia.org/wiki/Halton_sequence
public interface HaltonIterator extends BoundedPointIterator {

	public abstract int offset();

	public default double computePosition(int step, int base, double min, double max) {
		double position = min;
		double offset   = max - min;
		for (int index = this.index() * step + this.offset(); index > 0;) {
			offset /= base;
			position += offset * (index % base);
			index /= base;
		}
		return position;
	}
}