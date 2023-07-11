package builderb0y.bigglobe.math;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FastExpTest {

	public static float approx2(float f) { return FastExp.compute2(f); }
	public static double approx2(double d) { return FastExp.compute2(d); }
	public static float exact2(float f) { return (float)(Math.exp(f * FastExp.LOGE2D)); }
	public static double exact2(double d) { return Math.exp(d * FastExp.LOGE2D); }
	public static float approxE(float f) { return FastExp.computeE(f); }
	public static double approxE(double d) { return FastExp.computeE(d); }
	public static float exactE(float f) { return (float)(Math.exp(f)); }
	public static double exactE(double d) { return Math.exp(d); }

	public static class Accuracy {

		public static final float
			ERR = 0.00036F,
			STEP = 1.0F / 256.0F;

		@Test
		public void testCompute2F() {
			for (float f = -126.0F; f <= 127.0F; f += STEP) {
				assertEquals(1.0F, approx2(f) / exact2(f), ERR);
			}
		}

		@Test
		public void testComputeEF() {
			for (float f = -87.0F; f <= 88.0F; f += STEP) {
				assertEquals(1.0F, approxE(f) / exactE(f), ERR);
			}
		}

		@Test
		public void testCompute2D() {
			for (double d = -1022.0D; d <= 1023.0D; d += STEP) {
				assertEquals(1.0D, approx2(d) / exact2(d), ERR);
			}
		}

		@Test
		public void testComputeED() {
			for (double d = -708.0D; d <= 709.0D; d += STEP) {
				assertEquals(1.0D, approxE(d) / exactE(d), ERR);
			}
		}
	}

	public static class Speed {

		public static final float STEP = 1.0F / 524288.0F;

		@Test
		@Disabled
		public void time() {
			for (int trial = 1; trial <= 10; trial++) {
				System.out.printf("Trial %d:\n", trial);
				{
					long start = System.nanoTime();
					float sum = 0.0F;
					for (float f = -16.0F; f <= 16.0F; f += STEP) {
						sum += f;
					}
					long end = System.nanoTime();
					System.out.printf("control: %f in %,d ns\n", sum, end - start);
				}
				{
					long start = System.nanoTime();
					float sum = 0.0F;
					for (float f = -16.0F; f <= 16.0F; f += STEP) {
						sum += approx2(f);
					}
					long end = System.nanoTime();
					System.out.printf("Approx2F: %f in %,d ns\n", sum, end - start);
				}
				{
					long start = System.nanoTime();
					float sum = 0.0F;
					for (float f = -16.0F; f <= 16.0F; f += STEP) {
						sum += exact2(f);
					}
					long end = System.nanoTime();
					System.out.printf("Exact2F: %f in %,d ns\n", sum, end - start);
				}
				{
					long start = System.nanoTime();
					float sum = 0.0F;
					for (float f = -16.0F; f <= 16.0F; f += STEP) {
						sum += approxE(f);
					}
					long end = System.nanoTime();
					System.out.printf("ApproxEF: %f in %,d ns\n", sum, end - start);
				}
				{
					long start = System.nanoTime();
					float sum = 0.0F;
					for (float f = -16.0F; f <= 16.0F; f += STEP) {
						sum += exactE(f);
					}
					long end = System.nanoTime();
					System.out.printf("ExactEF: %f in %,d ns\n", sum, end - start);
				}
				{
					long start = System.nanoTime();
					double sum = 0.0D;
					for (double d = -16.0D; d <= 16.0D; d += STEP) {
						sum += approx2(d);
					}
					long end = System.nanoTime();
					System.out.printf("Approx2D: %f in %,d ns\n", sum, end - start);
				}
				{
					long start = System.nanoTime();
					double sum = 0.0D;
					for (double f = -16.0D; f <= 16.0D; f += STEP) {
						sum += exact2(f);
					}
					long end = System.nanoTime();
					System.out.printf("Exact2D: %f in %,d ns\n", sum, end - start);
				}
				{
					long start = System.nanoTime();
					double sum = 0.0D;
					for (double d = -16.0D; d <= 16.0D; d += STEP) {
						sum += approxE(d);
					}
					long end = System.nanoTime();
					System.out.printf("ApproxED: %f in %,d ns\n", sum, end - start);
				}
				{
					long start = System.nanoTime();
					double sum = 0.0D;
					for (double f = -16.0D; f <= 16.0D; f += STEP) {
						sum += exactE(f);
					}
					long end = System.nanoTime();
					System.out.printf("ExactED: %f in %,d ns\n", sum, end - start);
				}
			}
		}
	}
}