package builderb0y.bigglobe.math;

/**
fast approximations for log2(x) and ln(x).
accurate to within 0.0027 of the correct answer.
in other words, -0.0027 <= (approx - exact) <= 0.0027.
so... not quite as accurate as {@link FastExp},
but still decent enough imo.

special cases:
	if the input {@link Double#isNaN(double) is NaN}, then the result is {@link Double#NaN}.
	if the input is less than 0, then the result is {@link Double#NaN}.
	if the input is positive or negative 0, then the result is {@link Double#NEGATIVE_INFINITY}.
	if the input is <a href="https://en.wikipedia.org/wiki/Denormal_number">subnormal</a>
	(in other words, less than {@link Double#MIN_NORMAL} or {@link Float#MIN_NORMAL}),
	then the result is {@link Double#NEGATIVE_INFINITY}.
	if the input is {@link Double#POSITIVE_INFINITY},
	then the result is {@link Double#POSITIVE_INFINITY}.

algorithm:
	for base 2, the result is computed as log2(mantissa * 2 ^ exponent)
	= log2(mantissa) + log2(2 ^ exponent)
	= log2(mantissa) + exponent.
	the first part uses a cubic curve to get an
	approximation for log2(value) in the domain [1, 2].
	the 2nd part uses the bitwise representation
	of floats to extract the exponent directly.
*/
public class FastLog {

	public static final double
		LOGE2D, //ln(2)
		LOG2ED, //log2(e)
		TERM3D, //coefficient for x^3
		TERM2D, //coefficient for x^2
		TERM1D, //coefficient for x^1
		TERM0D; //coefficient for x^0
	public static final float LOGE2F, LOG2EF, TERM3F, TERM2F, TERM1F, TERM0F; //the above constants, cast to floats.

	static {
		final double ln2 = Math.log(2.0D);
		LOGE2D = ln2;
		LOG2ED = 1.0D / ln2;
		TERM3D = 1.5D / ln2 - 2.0D;
		TERM2D = 9.0D - 7.0D / ln2;
		TERM1D = 10.5D / ln2 - 12.0D;
		TERM0D = 4.9974D - 5.0D / ln2; //approximate average error from tests.
		LOGE2F = (float)(LOGE2D);
		LOG2EF = (float)(LOG2ED);
		TERM3F = (float)(TERM3D);
		TERM2F = (float)(TERM2D);
		TERM1F = (float)(TERM1D);
		TERM0F = (float)(TERM0D);
	}

	public static float compute2(float value) {
		if (!(value >= Float.MIN_NORMAL)) {
			return value >= 0.0F ? Float.NEGATIVE_INFINITY : Float.NaN;
		}
		if (value == Float.POSITIVE_INFINITY) return Float.POSITIVE_INFINITY;

		int bits = Float.floatToRawIntBits(value);
		int exponent = ((bits & 0x7F800000) >>> 23) - 127;
		bits = (bits & ~0x7F800000) | (127 << 23);
		float result = Float.intBitsToFloat(bits);
		result = ((TERM3F * result + TERM2F) * result + TERM1F) * result + TERM0F;
		return result + ((float)(exponent));
	}

	public static float computeE(float value) {
		return compute2(value) * LOGE2F;
	}

	public static double compute2(double value) {
		if (!(value >= Double.MIN_NORMAL)) {
			return value == 0.0D ? Double.NEGATIVE_INFINITY : Double.NaN;
		}
		if (value == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;

		long bits = Double.doubleToRawLongBits(value);
		long exponent = ((bits & 0x7FF0000000000000L) >>> 52) - 1023;
		bits = (bits & ~0x7FF0000000000000L) | (1023L << 52);
		double result = Double.longBitsToDouble(bits);
		result = ((TERM3D * result + TERM2D) * result + TERM1D) * result + TERM0D;
		return result + ((double)(exponent));

	}

	public static double computeE(double value) {
		return compute2(value) * LOGE2D;
	}
}