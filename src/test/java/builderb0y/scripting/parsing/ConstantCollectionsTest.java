package builderb0y.scripting.parsing;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.scripting.wrappers.ConstantMap;
import builderb0y.bigglobe.scripting.wrappers.ConstantSet;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.ConstantValue.NullConstantValue;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.TestCommon.assertSuccess;
import static builderb0y.scripting.bytecode.InsnTrees.*;
import static org.junit.jupiter.api.Assertions.*;

public class ConstantCollectionsTest {

	@Test
	public void testInflateDeflate() {
		RandomGenerator random = new SplittableRandom(12345L);
		for (int length = 0; length <= 100; length++) {
			ConstantValue[] input = new ConstantValue[length];
			Object[] expectedOutput = new Object[length];
			for (int trial = 0; trial < 100; trial++) {
				for (int index = 0; index < length; index++) {
					switch (random.nextInt(6)) {
						case 0 -> {
							switch (random.nextInt(5)) {
								case 0 -> {
									int value = random.nextInt();
									input[index] = constant(value);
									expectedOutput[index] = value;
								}
								case 1 -> {
									long value = random.nextLong();
									input[index] = constant(value);
									expectedOutput[index] = value;
								}
								case 2 -> {
									float value = Float.intBitsToFloat(random.nextInt());
									input[index] = constant(value);
									expectedOutput[index] = value;
								}
								case 3 -> {
									double value = Double.longBitsToDouble(random.nextLong());
									input[index] = constant(value);
									expectedOutput[index] = value;
								}
								case 4 -> {
									byte[] bytes = new byte[random.nextInt(10)];
									random.nextBytes(bytes);
									String value = new String(bytes, StandardCharsets.ISO_8859_1);
									input[index] = constant(value);
									expectedOutput[index] = value;
								}
							}
						}
						case 1 -> {
							boolean value = random.nextBoolean();
							input[index] = constant(value);
							expectedOutput[index] = value;
						}
						case 2 -> {
							byte value = (byte)(random.nextInt());
							input[index] = constant(value);
							expectedOutput[index] = value;
						}
						case 3 -> {
							short value = (short)(random.nextInt());
							input[index] = constant(value);
							expectedOutput[index] = value;
						}
						case 4 -> {
							char value = (char)(random.nextInt());
							input[index] = constant(value);
							expectedOutput[index] = value;
						}
						case 5 -> {
							input[index] = constant(null, TypeInfos.OBJECT);
							expectedOutput[index] = null;
						}
					}
				}
				ConstantValue[] inflated = JavaUtilScriptEnvironment.inflate(input);
				int inflatedLength = inflated.length;
				Object[] inflatedOutput = new Object[inflatedLength];
				for (int inflatedIndex = 0; inflatedIndex < inflatedLength; inflatedIndex++) {
					Object object = inflated[inflatedIndex] instanceof NullConstantValue ? null : inflated[inflatedIndex].asAsmObject();
					inflatedOutput[inflatedIndex] = object;
				}
				Object[] deflated = JavaUtilScriptEnvironment.deflate(inflatedOutput);
				assertArrayEquals(expectedOutput, deflated);
			}
		}
	}

	@Test
	public void testListsAndMapsAndSets() throws ScriptParsingException {
		assertSuccess(42,   "ConstantList.new(1, 2, 42, 6, 5).(2)");
		assertSuccess(true, "ConstantSet .new(1, 2, 42, 6, 5).contains(42)");
		assertSuccess(42,   "ConstantList list = new(1, 2, 42, 6, 5) list.(2)");
		assertSuccess(true, "ConstantSet  set  = new(1, 2, 42, 6, 5) set .contains(42)");
		assertSuccess(42,   "ConstantMap.new('a': 1, 'b': 2, 'c': 42, 'd': 6, 'e': 5).('c')");
		assertSuccess(42,   "ConstantMap map = new('a': 1, 'b': 2, 'c': 42, 'd': 6, 'e': 5) map.('c')");
		assertSuccess(true, "ConstantList.new(1, 2, 3) === ConstantList.new(1, 2, 3)");
		assertSuccess(true, "ConstantSet .new(1, 2, 3) === ConstantSet .new(1, 2, 3)");
		assertSuccess(true, "ConstantMap .new('a': 1, 'b': 2) === ConstantMap.new('a': 1, 'b': 2)");
		assertSuccess(Boolean.class, "ConstantList.new(true).(0).getClass()");
		assertSuccess(Byte.class, "ConstantList.new(0Y).(0).getClass()");
		assertSuccess(Short.class, "ConstantList.new(0S).(0).getClass()");
		assertSuccess(Integer.class, "ConstantList.new(0I).(0).getClass()");
	}

	@Test
	public void testConstantMaps() {
		testMap();
		testMap("a", 1);
		testMap("a", 1, "b", 2);
		testMap("a", 1, "b", 2, "c", 3);
		testMap("a", 1, "b", 2, "c", 3, "d", 4);
		testMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
		testMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
		testMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
		testMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);
		testMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
		testMap(new Object(), new Object());
		testMap(new Object(), new Object(), new Object(), new Object());
		testMap(new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
		testMap(null, null);
		testMap("a", null);
		testMap("a", null, "b", null);
		testMap(null, 1);
		testMapFail("a", 1, "a", 2);
		testMapFail("a", 1, "a", 1);
		testMap("a", 1, "b", 1);
		testMapFail(null, 1, null, 2);
		testMapFail(null, 1, null, 1);
		testMap(null, 1, "b", 2, "c", 3, "d", 4);
		testMap("a", 1, null, 2, "c", 3, "d", 4);
		testMap("a", 1, "b", 2, null, 3, "d", 4);
		testMap("a", 1, "b", 2, "c", 3, null, 4);
		testMapFail(1);
		testMapFail(1, 2, 3);
	}

	@Test
	public void testConstantSets() {
		testSet();
		testSet("a");
		testSet("a", "b");
		testSet("a", "b", "c");
		testSet("a", "b", "c", "d");
		testSet("a", "b", "c", "d", "e");
		testSet("a", "b", "c", "d", "e", "f");
		testSet("a", "b", "c", "d", "e", "f", "g");
		testSet("a", "b", "c", "d", "e", "f", "g", "h");
		testSet("a", "b", "c", "d", "e", "f", "g", "h", "i");
		testSet(new Object());
		testSet(new Object(), new Object());
		testSet(new Object(), new Object(), new Object());
		testSet(new Object(), new Object(), new Object(), new Object());
		testSet((Object)(null));
		testSet("a", null);
		testSet(null, "a");
		testSet("a", "b", null);
		testSet("a", null, "c");
		testSet(null, "b", "c");
		testSetFail("a", "a");
		testSetFail(null, null);
	}

	public static void testMap(Object... args) {
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

	public static void testSet(Object... args) {
		ConstantSet<Object> constantSet = new ConstantSet<>(args);
		HashSet<Object> nonConstantSet = new HashSet<>(args.length);
		for (int index = 0, length = args.length; index < length; index++) {
			Object element = args[index];
			nonConstantSet.add(element);
			assertTrue(constantSet.contains(element));
		}
		assertTrue(constantSet.equals(nonConstantSet) && nonConstantSet.equals(constantSet));
		int index = 0;
		for (Object element : constantSet) {
			assertEquals(args[index++], element);
		}
	}

	public static void testMapFail(Object... args) {
		try {
			new ConstantMap<>(args);
			fail("Map accepted arguments: " + Arrays.toString(args));
		}
		catch (IllegalArgumentException expected) {}
	}

	public static void testSetFail(Object... args) {
		try {
			new ConstantSet<>(args);
			fail("Set accepted arguments: " + Arrays.toString(args));
		}
		catch (IllegalArgumentException expected) {}
	}
}