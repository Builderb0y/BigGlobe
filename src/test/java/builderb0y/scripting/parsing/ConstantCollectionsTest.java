package builderb0y.scripting.parsing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.scripting.wrappers.ConstantMap;

import static builderb0y.scripting.TestCommon.assertSuccess;
import static org.junit.jupiter.api.Assertions.*;

public class ConstantCollectionsTest {

	@Test
	public void testListsAndMaps() throws ScriptParsingException {
		assertSuccess(42, "ConstantList.new(1, 2, 42, 6, 5).(2)");
		assertSuccess(42, "ConstantList list = new(1, 2, 42, 6, 5) list.(2)");
		assertSuccess(42, "ConstantMap.new('a': 1, 'b': 2, 'c': 42, 'd': 6, 'e': 5).('c')");
		assertSuccess(42, "ConstantMap map = new('a': 1, 'b': 2, 'c': 42, 'd': 6, 'e': 5) map.('c')");
		assertSuccess(true, "ConstantList.new(1, 2, 3) === ConstantList.new(1, 2, 3)");
		assertSuccess(true, "ConstantMap.new('a': 1, 'b': 2) === ConstantMap.new('a': 1, 'b': 2)");
	}

	@Test
	public void testConstantMaps() {
		test();
		test("a", 1);
		test("a", 1, "b", 2);
		test("a", 1, "b", 2, "c", 3);
		test("a", 1, "b", 2, "c", 3, "d", 4);
		test("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
		test("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
		test("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
		test("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);
		test("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
		test(new Object(), new Object());
		test(new Object(), new Object(), new Object(), new Object());
		test(new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
		test(null, null);
		test("a", null);
		test("a", null, "b", null);
		test(null, 1);
		testFail("a", 1, "a", 2);
		testFail("a", 1, "a", 1);
		test("a", 1, "b", 1);
		testFail(null, 1, null, 2);
		testFail(null, 1, null, 1);
		test(null, 1, "b", 2, "c", 3, "d", 4);
		test("a", 1, null, 2, "c", 3, "d", 4);
		test("a", 1, "b", 2, null, 3, "d", 4);
		test("a", 1, "b", 2, "c", 3, null, 4);
		testFail(1);
		testFail(1, 2, 3);
	}

	public static void test(Object... args) {
		ConstantMap<Object, Object> constantMap = new ConstantMap<>(args);
		HashMap<Object, Object> nonConstantMap = new HashMap<>(args.length);
		for (int index = 0, length = args.length; index < length;) {
			Object key = args[index++];
			Object value = args[index++];
			nonConstantMap.put(key, value);
			assertTrue(constantMap.containsKey(key));
			assertTrue(constantMap.containsValue(value));
			assertEquals(value, constantMap.get(key));
		}
		assertTrue(constantMap.equals(nonConstantMap) && nonConstantMap.equals(constantMap));
		int index = 0;
		for (Map.Entry<Object, Object> entry : constantMap.entrySet()) {
			assertEquals(args[index++], entry.getKey());
			assertEquals(args[index++], entry.getValue());
		}
	}

	public static void testFail(Object... args) {
		try {
			new ConstantMap<>(args);
			fail("Map accepted arguments: " + Arrays.toString(args));
		}
		catch (IllegalArgumentException expected) {}
	}
}