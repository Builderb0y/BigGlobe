package builderb0y.bigglobe.compat.voxy;

import java.io.*;
import java.util.Arrays;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.noise.Permuter;

import static org.junit.jupiter.api.Assertions.*;

public class BitStreamTest {

	@Test
	public void test() throws IOException {
		boolean[] booleans1 = new boolean[1048576];
		RandomGenerator random = new Permuter(12345L);
		for (int index = 0; index < booleans1.length; index++) {
			booleans1[index] = random.nextBoolean();
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BitOutputStream out = new BitOutputStream(new DataOutputStream(baos))) {
			for (boolean b : booleans1) {
				out.write(b);
			}
		}
		boolean[] booleans2 = new boolean[booleans1.length];
		try (BitInputStream in = new BitInputStream(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())))) {
			for (int index = 0; index < booleans2.length; index++) {
				booleans2[index] = in.readBit();
			}
		}
		assertTrue(Arrays.equals(booleans1, booleans2)); //do not print a million elements on failure.
	}
}