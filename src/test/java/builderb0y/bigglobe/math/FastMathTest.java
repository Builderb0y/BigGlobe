package builderb0y.bigglobe.math;

import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.environments.MathScriptEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class FastMathTest {

	@Test
	public void testBasicTrig() {
		checkRelative(-10.0D, 10.0D, 0.0625D, Math::cos, FastMath.Trig::fastCos, 0.005D);
		checkRelative(-10.0D, 10.0D, 0.0625D, Math::sin, FastMath.Trig::fastSin, 0.005D);
		checkProportional(-10.0D, 10.0D, 0.0625D, Math::tan, FastMath.Trig::fastTan, 0.005D);
	}

	@Test
	public void testInverseTrig() {
		checkRelative(-1.0D, 1.0D, 0.0625D, Math::asin, FastMath.Trig::fastAsin, 0.005D);
		checkRelative(-1.0D, 1.0D, 0.0625D, Math::acos, FastMath.Trig::fastAcos, 0.005D);
		checkRelative(-10.0D, 10.0D, 0.0625D, Math::atan, FastMath.Trig::fastAtan, 0.005D);
	}

	@Test
	public void testAtan2() {
		for (int x = -10; x <= 10; x++) {
			for (int y = -10; y <= 10; y++) {
				assertEquals(Math.atan2(y, x), FastMath.Trig.fastAtan2(x, y), 0.005D);
			}
		}
	}

	@Test
	public void testHyperbolic() {
		checkProportional(-4.0D, 4.0D, 0.0625D, Math::sinh, FastMath.Trig::fastSinh, 0.000005D);
		checkProportional(-4.0D, 4.0D, 0.0625D, Math::cosh, FastMath.Trig::fastCosh, 0.000005D);
		checkRelative(-4.0D, 4.0D, 0.0625D, Math::tanh, FastMath.Trig::fastTanh, 0.0000005D);
	}

	@Test
	public void testInverseHyperbolic() {
		checkRelative(-10.0D, 10.0D, 0.0625D, MathScriptEnvironment::asinh, FastMath.Trig::fastAsinh, 0.00005D);
		checkRelative(1.0D, 10.0D, 0.0625D, MathScriptEnvironment::acosh, FastMath.Trig::fastAcosh, 0.00005D);
		checkRelative(-1.0D + 0.015625D, 1.0D - 0.015625D, 0.015625D, MathScriptEnvironment::atanh, FastMath.Trig::fastAtanh, 0.00005D);
	}

	@Test
	public void testExp() {
		checkProportional(-4.0D, 4.0D, 0.0625D, Math::exp, FastMath.Exp::fastExp, 0.0000005D);
	}

	@Test
	public void testLog() {
		checkRelative(0.015625D, 10.0D, 0.015625D, Math::log, FastMath.Log::fastLog, 0.00005D);
	}

	public static void checkRelative(double start, double end, double step, DoubleUnaryOperator expected, DoubleUnaryOperator actual, double epsilon) {
		for (double value = start; value <= end; value += step) {
			assertEquals(expected.applyAsDouble(value), actual.applyAsDouble(value), epsilon);
		}
	}

	public static void checkProportional(double start, double end, double step, DoubleUnaryOperator expected, DoubleUnaryOperator actual, double epsilon) {
		for (double value = start; value <= end; value += step) {
			double expectedValue = expected.applyAsDouble(value);
			double actualValue = actual.applyAsDouble(value);
			if (expectedValue != actualValue) { //catches 0.0 / 0.0.
				assertEquals(1.0D, expectedValue / actualValue, epsilon);
			}
		}
	}
}