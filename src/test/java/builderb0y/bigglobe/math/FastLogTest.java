package builderb0y.bigglobe.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FastLogTest {

	public static float approx2(float f) { return FastLog.compute2(f); }
	public static float exact2(float f) { return (float)(Math.log(f) * FastExp.LOG2ED); }
	public static double approx2(double d) { return FastLog.compute2(d); }
	public static double exact2(double d) { return Math.log(d) * FastExp.LOG2ED; }
	public static float approxE(float f) { return FastLog.computeE(f); }
	public static float exactE(float f) { return (float)(Math.log(f)); }
	public static double approxE(double d) { return FastLog.computeE(d); }
	public static double exactE(double d) { return Math.log(d); }

	public static final float ERR = 0.0027F;

	@Test
	public void testCompute2F() {
		for (float f = Float.MIN_NORMAL; f <= Float.MAX_VALUE; f *= 1.001F) {
			assertEquals(0.0F, approx2(f) - exact2(f), ERR);
		}
	}

	@Test
	public void testComputeEF() {
		for (float f = Float.MIN_NORMAL; f <= Float.MAX_VALUE; f *= 1.001F) {
			assertEquals(0.0F, approxE(f) - exactE(f), ERR);
		}
	}

	@Test
	public void testCompute2D() {
		for (double d = Double.MIN_NORMAL; d <= Double.MAX_VALUE; d *= 1.001D) {
			assertEquals(0.0F, approx2(d) - exact2(d), ERR);
		}
	}

	@Test
	public void testComputeED() {
		for (double d = Double.MIN_NORMAL; d <= Double.MAX_VALUE; d *= 1.001D) {
			assertEquals(0.0F, approxE(d) - exactE(d), ERR);
		}
	}
}