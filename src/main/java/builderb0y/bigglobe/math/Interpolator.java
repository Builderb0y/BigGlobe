package builderb0y.bigglobe.math;

import builderb0y.scripting.environments.MathScriptEnvironment;

public class Interpolator {

	/**
	returns a value between low and high when value is between 0 and 1.
	mixLinear(a, b, 0.0) returns a.
	mixLinear(a, b, 1.0) returns b.
	*/
	public static float mixLinear(float low, float high, float value) {
		return (high - low) * value + low;
	}

	public static double mixLinear(double low, double high, double value) {
		return (high - low) * value + low;
	}

	public static float mixClamp(float low, float high, float value) {
		if (!(value > 0.0F)) return low;
		if (!(value < 1.0F)) return high;
		return mixLinear(low, high, value);
	}

	public static double mixClamp(double low, double high, double value) {
		if (!(value > 0.0D)) return low;
		if (!(value < 1.0D)) return high;
		return mixLinear(low, high, value);
	}

	public static float mixSmooth(float low, float high, float value) {
		if (!(value > 0.0F)) return low;
		if (!(value < 1.0F)) return high;
		return mixLinear(low, high, smooth(value));
	}

	public static double mixSmooth(double low, double high, double value) {
		if (!(value > 0.0D)) return low;
		if (!(value < 1.0D)) return high;
		return mixLinear(low, high, smooth(value));
	}

	public static float mixSmoother(float low, float high, float value) {
		if (!(value > 0.0F)) return low;
		if (!(value < 1.0F)) return high;
		return mixLinear(low, high, smoother(value));
	}

	public static double mixSmoother(double low, double high, double value) {
		if (!(value > 0.0D)) return low;
		if (!(value < 1.0D)) return high;
		return mixLinear(low, high, smoother(value));
	}

	/**
	returns a value between 0 and 1 when value is between low and high
	unmixLinear(a, b, a) returns 0.0
	unmixLinear(a, b, b) returns 1.0
	unmixLinear(a, b, mixLinear(a, b, f)) returns f.
	mixLinear(a, b, unmixLinear(a, b, f)) also returns f.
	results are undefined when low == high, including in the above examples.
	*/
	public static float unmixLinear(float low, float high, float value) {
		return (value - low) / (high - low);
	}

	public static double unmixLinear(double low, double high, double value) {
		return (value - low) / (high - low);
	}

	public static float unmixClamp(float low, float high, float value) {
		return clamp(0.0F, 1.0F, unmixLinear(low, high, value));
	}

	public static double unmixClamp(double low, double high, double value) {
		return clamp(0.0D, 1.0D, unmixLinear(low, high, value));
	}

	public static float unmixSmooth(float low, float high, float value) {
		return smoothClamp(unmixLinear(low, high, value));
	}

	public static double unmixSmooth(double low, double high, double value) {
		return smoothClamp(unmixLinear(low, high, value));
	}

	public static float unmixSmoother(float low, float high, float value) {
		return smoothClamp(unmixLinear(low, high, value));
	}

	public static double unmixSmoother(double low, double high, double value) {
		return smoothClamp(unmixLinear(low, high, value));
	}

	public static int clamp(int low, int high, int value) {
		return Math.max(Math.min(value, high), low);
	}

	public static long clamp(long low, long high, long value) {
		return Math.max(Math.min(value, high), low);
	}

	public static float clamp(float low, float high, float value) {
		return MathScriptEnvironment.max(MathScriptEnvironment.min(value, high), low);
	}

	public static double clamp(double low, double high, double value) {
		return MathScriptEnvironment.max(MathScriptEnvironment.min(value, high), low);
	}

	/**
	cubic interpolation, from https://www.paulinternet.nl/?page=bicubic
	a is the value at location -1
	b is the value at location 0
	c is the value at location 1
	d is the value at location 2
	f is the location we want to solve for.
	f MUST be in the range [0, 1].
	returns the value at location f
	if you plan on calling this method repeatedly for the same a, b, c, and d values (but different f values),
	then it is recommended to instead cache the terms directly (see below) and call combineCubicTerms() instead.
	*/
	public static double mixCubic(double a, double b, double c, double d, double f) {
		return b + 0.5D * f * (c - a + f * (2.0D * a - 5.0D * b + 4.0D * c - d + f * (3.0D * (b - c) + d - a)));
	}

	/**
	cubic interpolation based on terms rather than values.
	useful if the values themselves don't change between invocations, but f does.
	example usage:
		double term1 = cubicTerm1(a, b, c, d);
		double term2 = cubicTerm2(a, b, c, d);
		double term3 = cubicTerm3(a, b, c, d);
		double term4 = cubicTerm4(a, b, c, d);
		for (double f = a bunch of doubles between 0.0 and 1.0) {
			double interpolated = combineCubicTerms(term1, term2, term3, term4, f);
		}
	*/
	public static double combineCubicTerms(double term1, double term2, double term3, double term4, double f) {
		return ((term4 * f + term3) * f + term2) * f + term1;
	}

	//two of these methods do not make use of all their parameters.
	//this is intentional for readability.
	//hopefully JIT inlines this so it doesn't matter.
	//yes, I know I'm breaking my usual "optimize everything
	//at the source code level without relying on JIT" rule.
	public static double cubicTerm1(double a, double b, double c, double d) {
		return b;
	}

	public static double cubicTerm2(double a, double b, double c, double d) {
		return (c - a) * 0.5D;
	}

	public static double cubicTerm3(double a, double b, double c, double d) {
		return a - (2.5D * b) + (2.0D * c) - (0.5D * d);
	}

	public static double cubicTerm4(double a, double b, double c, double d) {
		return 1.5D * (b - c) + 0.5D * (d - a);
	}

	public static double cubicDerivative(double a, double b, double c, double d, double f) {
		//1.5df^2 - 4.5cf^2 + 4.5bf^2 - 1.5af^2 - df + 4.0cf - 5.0bf + 2.0af + 0.5c - 0.5a
		//(1.5d - 4.5c + 4.5b - 1.5a) * f^2 - df + 4.0cf - 5.0bf + 2.0af + 0.5c - 0.5a
		//(1.5 * (d - a) + 4.5 * (b - c)) * f^2 - df + 4.0cf - 5.0bf + 2.0af + 0.5c - 0.5a
		//(1.5 * (d - a) + 4.5 * (b - c)) * f^2 + (-d + 4.0c - 5.0b + 2.0a) * f + 0.5c - 0.5a
		//(1.5 * (d - a) + 4.5 * (b - c)) * f^2 + (2.0a - 5.0b + 4.0c - d) * f + 0.5c - 0.5a
		//((1.5 * (d - a) + 4.5 * (b - c)) * f + (2.0a - 5.0b + 4.0c - d)) * f + 0.5 * (c - a)
		return ((1.5D * (d - a) + 4.5D * (b - c)) * f + (2.0D * a - 5.0D * b + 4.0D * c - d)) * f + 0.5D * (c - a);
	}

	public static double cubicDerivativeTerm1(double a, double b, double c, double d) {
		return 1.5D * (d - a) + 4.5D * (b - c);
	}

	public static double cubicDerivativeTerm2(double a, double b, double c, double d) {
		return 2.0D * a - 5.0D * b + 4.0D * c - d;
	}

	public static double cubicDerivativeTerm3(double a, double b, double c, double d) {
		return 0.5D * (c - a);
	}

	public static double combineCubicDerivativeTerms(double term1, double term2, double term3, double f) {
		return (term1 * f + term2) * f + term3;
	}

	/**
	produces a smooth curve in the domain [0, 1].
	computed using the cubic polynomial -2x^3 + 3x^2.
	in this domain, it looks very similar to cos(pi * value) * -0.5 + 0.5.
	fixed points:
	smooth(0) = 0
	smooth(0.5) = 0.5
	smooth(1) = 1
	slope/derivative: -6x^2 + 6x
	smooth'(0) = 0
	smooth'(0.5) = 1.5
	smooth'(1) = 0
	*/
	public static double smooth(double value) {
		return (-2.0D * value + 3.0D) * value * value;
	}

	public static float smooth(float value) {
		return (-2.0F * value + 3.0F) * value * value;
	}

	public static double smoothClamp(double value) {
		if (value <= 0.0D) return 0.0D;
		if (value >= 1.0D) return 1.0D;
		return smooth(value);
	}

	public static float smoothClamp(float value) {
		if (value <= 0.0F) return 0.0F;
		if (value >= 1.0F) return 1.0F;
		return smooth(value);
	}

	public static double smootherClamp(double value) {
		if (value <= 0.0D) return 0.0D;
		if (value >= 1.0D) return 1.0D;
		return smoother(value);
	}

	public static float smootherClamp(float value) {
		if (value <= 0.0F) return 0.0F;
		if (value >= 1.0F) return 1.0F;
		return smoother(value);
	}

	public static double smoothDerivative(double value) {
		return (-6.0D * value + 6.0D) * value;
	}

	/**
	similar to {@link #smooth(double)}, but smooth only ensures
	that the first derivative is 0 when value = 0 or value = 1.
	by contrast, smoother() ensures that the 2nd
	derivative is 0 at these two locations as well.
	*/
	public static double smoother(double value) {
		return ((6.0D * value - 15.0D) * value + 10.0D) * value * value * value;
	}

	public static float smoother(float value) {
		return ((6.0F * value - 15.0F) * value + 10.0F) * value * value * value;
	}

	public static double smootherDerivative(double value) {
		return ((30.0D * value - 60.0D) * value + 30.0D) * value * value;
	}
}