package builderb0y.bigglobe.compat.voxy;

import java.io.*;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.noise.Permuter;

import static org.junit.jupiter.api.Assertions.*;

public class BitStreamTest {

	@Test
	public void test() throws IOException {
		RandomGenerator random = new Permuter(12345L);
		for (int length = 0; length <= 1024; length++) {
			boolean[] booleans1 = new boolean[length];
			for (int index = 0; index < length; index++) {
				booleans1[index] = random.nextBoolean();
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (BitOutputStream out = new BitOutputStream(new DataOutputStream(baos))) {
				for (boolean b : booleans1) {
					out.write(b);
				}
			}
			boolean[] booleans2 = new boolean[length];
			try (BitInputStream in = new BitInputStream(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())))) {
				for (int index = 0; index < length; index++) {
					booleans2[index] = in.readBit();
				}
			}
			assertArrayEquals(booleans1, booleans2);
		}
	}
}