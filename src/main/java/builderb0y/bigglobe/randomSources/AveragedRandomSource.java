package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;

/**
implementation of {@link RandomSource} which produces numbers in a configurable range,
but ensures that the produced numbers have a pre-determined average too.
for example, one could use this class to produce numbers
between 0 and 10, but with an average of just 2.
in this case, low numbers will be produced often,
and high numbers will be produced more rarely.

theory:

first, what is the average of all real numbers between 0 and 1?
we can solve this quite easily with the integral:
	∫[0, 1] (x) * dx = (x^2/2) [0, 1] = (1^2/2) - (0^2/2) = 1/2
this is also the same as the area under the line y = x in the domain [0, 1].

but what if we want a different average?
we can imagine taking a different curve which passes through (0, 0) and (1, 1).
for example, maybe we have the curve x^2 instead.
what's the average of that?
	∫[0, 1] (x^2) * dx = (x^3/3) [0, 1] = (1^3/3) - (0^3/3) = 1/3
we can generalize this quite easily to other powers
to force the average to be whatever we want.

but personally, I don't like x^n because it's not symmetric.
by this I mean that for x^2 for example, x^2 approaches 0 as x
approaches 0 much faster than x^2 approaches 1 as x approaches 1.
we could try other types of functions,
but tl;dr: the one I like the best is:
	bias(x, a) = (ax + x) / (ax + 1) for some constant a.

so what's the average of that, as a function of a?
this is a messy integral, but ultimately it evaluates to:
	average(a) = 1 / a - ln(a + 1) / a^2 - ln(a + 1) / a + 1
this allows us to predict the average given the constant a,
but to find the value of a which produces the average we want,
we need to find an inverse function of average().

this is where I got a bit stuck, but through trial and error,
I managed to find a very close approximation for average() as:
	approxAverage(a) = 1 / (1 + (a + 1) ^ (-2/3))
and this has the trivial inverse function:
	inverseApproxAverage(a) = (1 / a - 1) ^ (-3/2) - 1

so, that is what I'm using here.
	bias(random.nextFloat(), inverseApproxAverage(desiredAverage))
will produce numbers which, when averaged together, produce desiredAverage.
the final step is to scale the result to arbitrary ranges, not necessarily [0, 1]:
	mix(min, max, bias(random.nextFloat(), inverseApproxAverage(unmix(min, max, desiredAverage))))

oh, and if anyone has a more exact inverse function of average(), please let me know.
*/
public class AveragedRandomSource implements RandomSource {

	public final double min;
	public final double max;
	public final @VerifySorted(greaterThan = "min", lessThan = "max") double average;
	public final transient double coefficient;

	public AveragedRandomSource(double min, double max, double average) {
		this.min = min;
		this.max = max;
		this.average = average;
		this.coefficient = computeCoefficient(Interpolator.unmixLinear(min, max, average));
	}

	public static double computeCoefficient(double average) {
		double firstPart = 1.0D / average - 1.0D;
		//secondPart = firstPart ^ (-3/2)
		double secondPart = 1.0D / Math.sqrt(firstPart * firstPart * firstPart);
		//final result
		return secondPart - 1.0D;
	}

	public static double applyBias(double unbiased, double coefficient) {
		return (coefficient * unbiased + unbiased) / (coefficient * unbiased + 1.0D);
	}

	public double curve(double unbiased) {
		return this.mix(applyBias(unbiased, this.coefficient));
	}

	@Override
	public double get(long seed) {
		return this.curve(Permuter.nextPositiveDouble(seed));
	}

	@Override
	public double get(RandomGenerator random) {
		return this.curve(random.nextDouble());
	}

	@Override
	public double minValue() {
		return this.min;
	}

	@Override
	public double maxValue() {
		return this.max;
	}
}