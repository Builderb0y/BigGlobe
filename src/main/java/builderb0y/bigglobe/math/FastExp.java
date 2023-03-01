package builderb0y.bigglobe.math;

import static builderb0y.bigglobe.math.BigGlobeMath.floorI;

/**
fast approximations for 2^x and e^x.
accurate to within 3.6 parts in 10000.
in other words, (1 - 3.6 / 10000) <= (approx / exact) <= (1 + 3.6 / 10000).

special cases:
	if the input {@link Double#isNaN(double) is NaN}, then the result is {@link Double#NaN}.
	if the result would be <a href="https://en.wikipedia.org/wiki/Denormal_number">subnormal</a>
	(in other words, less than {@link Double#MIN_NORMAL} or {@link Float#MIN_NORMAL}),
	then positive 0 is returned.
	if the result would be too big to fit in a float or double,
	then {@link Double#POSITIVE_INFINITY} is returned
	to be consistent with {@link Math#exp(double)}.

algorithm:
	for base 2, the result is computed as 2 ^ ((value mod 1) + floor(value))
	= (2 ^ (value mod 1)) * (2 ^ floor(value))
	the first part uses a cubic curve to get an
	approximation for 2 ^ value in the domain [0, 1].
	the 2nd part uses bitwise tricks to modify the exponent of the result.
	adding N to the exponent is the same as multiplying the result by 2^N.

	for base e, the result is computed as 2 ^ (value * log_2(e)) using the above tactic.
*/
public class FastExp {

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
		TERM3D = 3.0D * ln2 - 2.0D;
		TERM2D = -4.0D * ln2 + 3.0D;
		TERM1D = ln2;
		TERM0D = 1.000356D; //approximate average error from tests.
		LOGE2F = (float)(LOGE2D);
		LOG2EF = (float)(LOG2ED);
		TERM3F = (float)(TERM3D);
		TERM2F = (float)(TERM2D);
		TERM1F = (float)(TERM1D);
		TERM0F = (float)(TERM0D);
	}

	public static float compute2(float value) {
		if (Float.isNaN(value)) return Float.NaN;
		if (value < Float.MIN_EXPONENT) return 0.0F;
		//Float.MAX_VALUE would be closer to the true mathematical value,
		//but we return Float.POSITIVE_INFINITY instead to ensure consistency with Math.exp().
		if (value >= Float.MAX_EXPONENT + 1) return Float.POSITIVE_INFINITY;

		int floor = floorI(value);
		value -= floor;
		float cubicCurve = ((TERM3F * value + TERM2F) * value + TERM1F) * value + TERM0F;
		int bits = Float.floatToRawIntBits(cubicCurve);
		bits += floor << 23;
		return Float.intBitsToFloat(bits);
	}

	public static float computeE(float value) {
		return compute2(value * LOG2EF);
	}

	public static double compute2(double value) {
		if (Double.isNaN(value)) return Double.NaN;
		if (value < Double.MIN_EXPONENT) return 0.0F;
		//Double.MAX_VALUE would be closer to the true mathematical value,
		//but we return Double.POSITIVE_INFINITY instead to ensure consistency with Math.exp().
		if (value >= Double.MAX_EXPONENT + 1) return Double.POSITIVE_INFINITY;

		int floor = floorI(value);
		value -= floor;
		double cubicCurve = ((TERM3D * value + TERM2D) * value + TERM1D) * value + TERM0D;
		long bits = Double.doubleToRawLongBits(cubicCurve);
		bits += ((long)(floor)) << 52;
		return Double.longBitsToDouble(bits);
	}

	public static double computeE(double value) {
		return compute2(value * LOG2ED);
	}
}