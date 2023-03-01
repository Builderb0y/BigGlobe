package builderb0y.bigglobe.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModulusTest {

	@Test
	public void testModulus() {
		for (int b = -16; b <= 16; b++) {
			for (int a = -16; a <= 16; a++) {
				int expected = b == 0 ? 0 : Math.floorMod(a, b);
				assertEquals(expected, BigGlobeMath.modulus(a, b));
				assertEquals((long)(expected), BigGlobeMath.modulus((long)(a), (long)(b)));
				assertEquals((float)(expected), BigGlobeMath.modulus((float)(a), (float)(b)), 0.0F);
				assertEquals((double)(expected), BigGlobeMath.modulus((double)(a), (double)(b)), 0.0D);
				assertEquals(Float.floatToRawIntBits((float)(b)) >>> 31, Float.floatToRawIntBits(BigGlobeMath.modulus((float)(a), (float)(b))) >>> 31);
				assertEquals(Double.doubleToRawLongBits((double)(b)) >>> 63, Double.doubleToRawLongBits(BigGlobeMath.modulus((double)(a), (double)(b))) >>> 63);
			}
		}
	}
}