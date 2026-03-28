package builderb0y.bigglobe.math.pointSequences;

//http://extremelearning.com.au/unreasonable-effectiveness-of-quasirandom-sequences/
public interface AdditiveRecurrenceIterator extends BoundedPointIterator {

	public static double checkSeed(double seed) {
		if (seed >= 0.0D && seed < 1.0D) return seed;
		else throw new IllegalArgumentException(Double.toString(seed));
	}
}