package builderb0y.scripting.bytecode.tree.instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import builderb0y.scripting.bytecode.tree.instructions.binary.SignedLeftShiftInsnTree;

import static org.junit.jupiter.api.Assertions.*;

public class ScalbTest {

	@Test
	public void testF() {
		for (int startPower = -256; startPower <= 256; startPower++) {
			for (float one = 1.0F; one < 2.0F; one += 0.25F) {
				float start = Math.scalb(one, startPower);
				for (int power = -512; power <= 512; power++) {
					float expected = Math.scalb(start, power);
					float actual = SignedLeftShiftInsnTree.shift(start, power);
					//allow up to 1 ulp of difference to account for differences in rounding.
					if (actual != expected && actual != Math.nextUp(expected) && actual != Math.nextDown(expected)) {
						SignedLeftShiftInsnTree.shift(start, power); //breakpoint.
						fail();
					}
				}
			}
		}
	}

	@Test
	public void testD() {
		for (int startPower = -2048; startPower <= 2048; startPower++) {
			for (double one = 1.0D; one < 2.0D; one += 0.25D) {
				double start = Math.scalb(one, startPower);
				for (int power = -4096; power <= 4096; power++) {
					double expected = Math.scalb(start, power);
					double actual = SignedLeftShiftInsnTree.shift(start, power);
					//allow up to 1 ulp of difference to account for differences in rounding.
					if (actual != expected && actual != Math.nextUp(expected) && actual != Math.nextDown(expected)) {
						SignedLeftShiftInsnTree.shift(start, power); //breakpoint.
						fail();
					}
				}
			}
		}
	}

	public static float blackHoleF;
	public static double blackHoleD;

	@Test
	@Disabled
	public void testSpeedF() {
		for (int trial = 1; trial <= 10; trial++) {
			System.out.println("Trial " + trial + ':');

			long startTime = System.nanoTime();
			for (float one = 1.0F; one < 2.0F; one += 1.0F / 1048576.0F) {
				for (int power = -256; power <= 256; power++) {
					blackHoleF = Math.scalb(one, power);
				}
			}
			long endTime = System.nanoTime();
			System.out.printf("scalbF: %,d ns.\n", endTime - startTime);

			startTime = System.nanoTime();
			for (float one = 1.0F; one < 2.0F; one += 1.0F / 1048576.0F) {
				for (int power = -256; power <= 256; power++) {
					blackHoleF = SignedLeftShiftInsnTree.shift(one, power);
				}
			}
			endTime = System.nanoTime();
			System.out.printf("shiftF: %,d ns.\n", endTime - startTime);
		}
	}

	@Test
	@Disabled
	public void testSpeedD() {
		for (int trial = 1; trial <= 10; trial++) {
			System.out.println("Trial " + trial + ':');

			long startTime = System.nanoTime();
			for (double one = 1.0D; one < 2.0D; one += 1.0D / 16384.0D) {
				for (int power = -2048; power <= 2048; power++) {
					blackHoleD = Math.scalb(one, power);
				}
			}
			long endTime = System.nanoTime();
			System.out.printf("scalbD: %,d ns.\n", endTime - startTime);

			startTime = System.nanoTime();
			for (double one = 1.0D; one < 2.0D; one += 1.0D / 16384.0D) {
				for (int power = -2048; power <= 2048; power++) {
					blackHoleD = SignedLeftShiftInsnTree.shift(one, power);
				}
			}
			endTime = System.nanoTime();
			System.out.printf("shiftD: %,d ns.\n", endTime - startTime);
		}
	}
}