package builderb0y.bigglobe.noise;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.function.IntFunction;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NumberArrayTest {

	@Test
	public void testBooleans() {
		RandomGenerator random = new SplittableRandom(12345L);
		NumberArray.Direct.Manager manager = NumberArray.Direct.Manager.INSTANCES.get();
		int used = manager.used;
		try {
			testBooleans(random, NumberArray::allocateBooleansHeap);
			testBooleans(random, NumberArray::allocateBooleansDirect);
		}
		finally {
			//only free at the end, because I want to test
			//cases where the backing direct array is not
			//empty when the NumberArray using it is allocated.
			manager.used = used;
		}
	}

	public static void testBooleans(RandomGenerator random, IntFunction<NumberArray> allocator) {
		for (int length = 1; length <= 100; length++) {
			NumberArray wrapped = allocator.apply(length);
			wrapped.fill(false);
			boolean[] unwrapped = new boolean[length];
			for (int operationTrial = 0; operationTrial < 100; operationTrial++) {
				if (random.nextBoolean()) {
					int index = random.nextInt(length);
					boolean value = random.nextBoolean();
					wrapped.setZ(index, value);
					unwrapped[index] = value;
				}
				else {
					int index1 = random.nextInt(length + 1);
					int index2 = random.nextInt(length + 1);
					if (index2 < index1) {
						int tmp = index1;
						index1 = index2;
						index2 = tmp;
					}
					boolean value = random.nextBoolean();
					wrapped.fillFromTo(index1, index2, value);
					Arrays.fill(unwrapped, index1, index2, value);
				}
				checkEqual(wrapped, unwrapped);
			}
		}
	}

	public static void checkEqual(NumberArray wrapped, boolean[] unwrapped) {
		int length = unwrapped.length;
		assertEquals(length, wrapped.length());
		for (int index = 0; index < length; index++) {
			assertTrue(unwrapped[index] == wrapped.getZ(index));
		}
	}
}