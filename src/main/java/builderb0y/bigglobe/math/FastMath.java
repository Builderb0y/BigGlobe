package builderb0y.bigglobe.math;

public class FastMath {

	public static class Trig {

		public static double cosCurve(double number) {
			number *= number;
			return (1.0D - 16.0D * number) * (1.0D - (16.0D - 4.0D * Math.PI) * number);
		}

		public static double fastSin(double angle) {
			angle *= 1.0D / BigGlobeMath.TAU;
			angle -= Math.floor(angle);
			return angle <= 0.5D ? cosCurve(angle - 0.25D) : -cosCurve(angle - 0.75D);
		}

		public static double fastCos(double angle) {
			return fastSin(angle + Math.PI * 0.5D);
		}

		public static double fastTan(double angle) {
			return fastSin(angle) / fastCos(angle);
		}

		public static double fastAsin(double x) {
			final double t1 = 1.0D / (Math.PI * 0.5D - 1.0D);
			final double t5 = t1 - 1.0D;
			final double t7 = t5 / -3.0D;
			final double t3 = t7 * 5.0D;
			double x2 = x * x;
			double circle = Math.PI * 0.5D - Math.sqrt(1.0D - x2);
			double poly = (((t7 * x2 + t5) * x2 + t3) * x2 + t1) * x;
			return poly * circle;
		}

		public static double fastAcos(double x) {
			return Math.PI * 0.5D - fastAsin(x);
		}

		public static double fastAtan(double x) {
			double x2 = x * x;
			double common = x / Math.sqrt((4.0D / (Math.PI * Math.PI)) * x2 + 1.0D);
			return common + (common - x) / (x2 * (16.0D / 15.0D) + (5.0D / 3.0D));
		}

		public static double fastAtan2(double x, double y) {
			//note: don't replace with copySign(),
			//because that handles the case where y == 0 differently.
			if (x == 0.0D) return (Math.PI * 0.5D) * Math.signum(y);
			double result = fastAtan(y / x);
			if (x < 0.0D) result += Math.copySign(Math.PI, y);
			return result;
		}

		public static double fastSinh(double x) {
			double exp = Exp.fastExp(x);
			return (exp - 1.0D / exp) * 0.5D;
		}

		public static double fastCosh(double x) {
			double exp = Exp.fastExp(x);
			return (exp + 1.0D / exp) * 0.5D;
		}

		public static double fastTanh(double x) {
			return 2.0D / (Exp.fastExp2(x * (-2.0D * Exp.LOG2E)) + 1.0D) - 1.0D;
		}

		public static double fastAsinh(double x) {
			return Log.fastLog(Math.sqrt(x * x + 1.0D) + x);
		}

		public static double fastAcosh(double x) {
			return Log.fastLog(Math.sqrt(x * x - 1.0D) + x);
		}

		public static double fastAtanh(double x) {
			//alternate form: log(2 / (1 - x) - 1) * 0.5
			return Log.fastLog((1.0D + x) / (1.0D - x)) * 0.5D;
		}
	}

	/**
	fast approximations for 2^x and e^x.
	accurate to within 5 parts in 10,000,000
	in other words, 0.9999995 <= (approx / exact) <= 1.0000005.

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
		the first part uses a quintic curve to get an
		approximation for 2 ^ value in the domain [0, 1].
		the 2nd part uses bitwise tricks to modify the exponent of the result.
		adding N to the exponent is the same as multiplying the result by 2^N.

		for base e, the result is computed as 2 ^ (value * log_2(e)) using the above tactic.
	*/
	public static class Exp {

		public static final double
			LOGE2 = Math.log(2.0D), //ln(2)
			LOG2E = 1.0D / LOGE2, //log2(e)
			TERM0 = 1.0D,
			TERM1 = 0.6931471805599453D,
			TERM2 = 0.24017440574159999D,
			TERM3 = 0.055811747933191217D,
			TERM4 = 0.008970203549168332D,
			TERM5 = 0.0018964622161163903D;

		public static double fastExp2(double value) {
			if (Double.isNaN(value)) return Double.NaN;
			if (value < Double.MIN_EXPONENT) return 0.0F;
			//Double.MAX_VALUE would be closer to the true mathematical value,
			//but we return Double.POSITIVE_INFINITY instead to ensure consistency with Math.exp().
			if (value >= Double.MAX_EXPONENT + 1) return Double.POSITIVE_INFINITY;

			double floor = Math.floor(value);
			value -= floor;
			double cubicCurve = ((((TERM5 * value + TERM4) * value + TERM3) * value + TERM2) * value + TERM1) * value + TERM0;
			long bits = Double.doubleToRawLongBits(cubicCurve);
			bits += ((long)(floor)) << 52;
			return Double.longBitsToDouble(bits);
		}

		public static double fastExp(double value) {
			return fastExp2(value * LOG2E);
		}
	}

	/**
	fast approximations for log2(x) and ln(x).
	accurate to within about 0.00005D of the correct answer.
	in other words, -0.00005D <= (approx - exact) <= 0.00005D

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
		the first part uses a quintic curve to get an
		approximation for log2(value) in the domain [1, 2].
		the 2nd part uses the bitwise representation
		of floats to extract the exponent directly.
	*/
	public static class Log {

		public static final double
			LOGE2 = Math.log(2.0D), //ln(2)
			LOG2E = 1.0D / LOGE2, //log2(e)
			TERM0 = -2.816159410743029D,
			TERM1 =  5.136019940826246D,
			TERM2 = -3.5965971344845508D,
			TERM3 =  1.651994396722813D,
			TERM4 = -0.42017514047089418D,
			TERM5 =  0.044917348149390499D;

		public static double fastLog2(double value) {
			if (!(value >= Double.MIN_NORMAL)) {
				return value >= 0.0D ? Double.NEGATIVE_INFINITY : Double.NaN;
			}
			if (value == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;

			long bits = Double.doubleToRawLongBits(value);
			long exponent = ((bits & 0x7FF0000000000000L) >>> 52) - 1023;
			bits = (bits & ~0x7FF0000000000000L) | (1023L << 52);
			double result = Double.longBitsToDouble(bits);
			result = ((((TERM5 * result + TERM4) * result + TERM3) * result + TERM2) * result + TERM1) * result + TERM0;
			return result + ((double)(exponent));
		}

		public static double fastLog(double value) {
			return fastLog2(value) * LOGE2;
		}
	}
}