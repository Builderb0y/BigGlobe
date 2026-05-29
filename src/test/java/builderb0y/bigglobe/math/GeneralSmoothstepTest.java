package builderb0y.bigglobe.math;

import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.math.GeneralSmoothstep.SmoothstepOperator;

import static org.junit.jupiter.api.Assertions.*;

public class GeneralSmoothstepTest {

	@Test
	public void test() {
		testEquality(0, 0, DoubleUnaryOperator.identity());
		testEquality(1, 0, (double x) -> x * x);
		testEquality(0, 1, (double x) -> x * (2.0D - x));
		testEquality(1, 1, (double x) -> x * x * (x * -2.0D + 3.0D));
		testEquality(2, 0, (double x) -> x * x * x);
		testEquality(0, 2, (double x) -> 1.0D - (1.0D - x) * (1.0D - x) * (1.0D - x));
		testEquality(2, 2, (double x) -> ((x * 6.0D - 15.0D) * x + 10.0) * x * x * x);
	}

	public static void testEquality(int lower, int upper, DoubleUnaryOperator hardCoded) {
		SmoothstepOperator auto = GeneralSmoothstep.getOperator(lower, upper);
		for (int in = 0; in <= 50; in++) {
			double out = auto.applyAsDouble(in / 50.0D);
			assertEquals(out, hardCoded.applyAsDouble(in / 50.0D), 1.0e-7D);
			assertEquals(out, (double)(auto.applyAsFloat(in / 50.0F)), 1.0e-7D);
		}
		assertEquals(0.0D, auto.applyAsDouble(-1.0D));
		assertEquals(1.0D, auto.applyAsDouble(+2.0D));
		assertEquals(0.0F, auto.applyAsFloat(-1.0F));
		assertEquals(1.0F, auto.applyAsFloat(+2.0F));
	}
}