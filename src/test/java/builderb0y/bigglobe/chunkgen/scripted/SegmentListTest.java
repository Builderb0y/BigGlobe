package builderb0y.bigglobe.chunkgen.scripted;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SegmentListTest {

	@Test
	public void test() {
		RandomGenerator random = new SplittableRandom(12345L);
		Object shared = new Object();
		for (int minY = 0; minY < 100; minY++) {
			for (int maxY = minY; maxY < 100; maxY++) {
				SegmentList<Object> wrapped = new SegmentList<>(minY, maxY);
				Object[] unwrapped = new Object[maxY - minY + 1];
				for (int operation = 0; operation < 100; operation++) {
					int low = random.nextInt(minY, maxY + 1);
					int high = random.nextInt(minY, maxY + 1);
					if (low > high) {
						int tmp = high;
						high = low;
						low = tmp;
					}
					Object object = switch (random.nextInt(3)) {
						case 0 -> new Object();
						case 1 -> shared;
						case 2 -> null;
						default -> throw new AssertionError("RNG fail!");
					};
					if (object != null) wrapped.addSegment(low, high, object);
					else wrapped.removeSegment(low, high);
					fill(unwrapped, low - minY, high - minY, object);
					checkEqual(wrapped, unwrapped, minY);
				}
			}
		}
	}

	public static void fill(Object[] array, int from, int to, Object value) {
		for (int index = from; index <= to; index++) {
			array[index] = value;
		}
	}

	public static void checkEqual(SegmentList<Object> wrapped, Object[] unwrapped, int offset) {
		for (int y = 0; y < 100; y++) {
			Object actual = wrapped.getOverlappingObject(y);
			if (actual != null) {
				assertSame(unwrapped[y - offset], actual);
			}
			else {
				assertTrue(y - offset < 0 || y - offset >= unwrapped.length || unwrapped[y - offset] == null);
			}
		}
	}

	@Test
	public void testGetTopOrBottomOfSegment() {
		SegmentList<Object> list = new SegmentList<>(0, 10);
		list.addSegment(5, 5, new Object());
		for (int start = -1; start <= 11; start++) {
			int desired = start < 5 ? Integer.MIN_VALUE : start == 5 ? 5 : 6;
			assertEquals(desired, list.getTopOrBottomOfSegment(start, false, Integer.MIN_VALUE));
			desired = start > 5 ? Integer.MAX_VALUE : start == 5 ? 5 : 4;
			assertEquals(desired, list.getTopOrBottomOfSegment(start, true, Integer.MAX_VALUE));
		}
		list.clear();
		list.addSegment(2, 2, new Object());
		list.addSegment(7, 7, new Object());
		assertEquals(3, list.getTopOrBottomOfSegment(5, false, Integer.MIN_VALUE));
		assertEquals(6, list.getTopOrBottomOfSegment(5, true, Integer.MAX_VALUE));
	}
}