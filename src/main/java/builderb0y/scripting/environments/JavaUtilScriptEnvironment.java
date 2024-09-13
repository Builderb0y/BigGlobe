package builderb0y.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.scripting.wrappers.ArrayWrapper;
import builderb0y.bigglobe.scripting.wrappers.ConstantMap;
import builderb0y.bigglobe.scripting.wrappers.ConstantSet;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.ConstantValue.NullConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.collections.NormalListMapGetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.MemberKeywordHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler.Named;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.special.ConstantMapSyntax;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class JavaUtilScriptEnvironment {

	public static final MethodInfo
		MAP_GET       = MethodInfo.getMethod(Map      .class, "get"),
		MAP_PUT       = MethodInfo.getMethod(Map      .class, "put"),
		MAP_ENTRY_GET = MethodInfo.getMethod(Map.Entry.class, "getValue"),
		MAP_ENTRY_SET = MethodInfo.inCaller("setEntryValue"),
		LIST_GET      = MethodInfo.getMethod(List     .class, "get"),
		LIST_SET      = MethodInfo.getMethod(List     .class, "set"),
		CONSTANT_LIST = MethodInfo.inCaller("constantList"),
		CONSTANT_MAP  = MethodInfo.inCaller("constantMap"),
		CONSTANT_SET  = MethodInfo.inCaller("constantSet");

	@Deprecated //use withRandom() or withoutRandom() instead.
	public static final MutableScriptEnvironment ALL = (
		new MutableScriptEnvironment()
		.addMethodInvokes(Object.class, "toString", "equals", "hashCode", "getClass")
		.addType("Iterator", Iterator.class)
		.addMethodInvokes(Iterator.class, "hasNext", "next", "remove")
		.addType("ListIterator", ListIterator.class)
		.addMethodInvokes(ListIterator.class, "hasPrevious", "previous", "nextIndex", "previousIndex", "set", "add")
		.addType("Map", Map.class)
		.addMethodMultiInvokes(Map.class, "size", "isEmpty", "containsKey", "containsValue", "get", "put", "remove", "putAll", "clear", "keySet", "values", "entrySet", "getOrDefault", "putIfAbsent", "replace")
		.addFieldInvokes(Map.class, "size", "isEmpty")
		.addMethod(TypeInfo.of(Map.class), "", new Named("Map.(key)", (parser, receiver, name, mode, arguments) -> {
			InsnTree key = ScriptEnvironment.castArgument(parser, "", TypeInfos.OBJECT, CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(
				NormalListMapGetterInsnTree.from(receiver, MAP_GET, key, MAP_PUT, "Map", mode),
				key != arguments[0]
			);
		}))
		.addType("MapEntry", Map.Entry.class)
		.addMethodInvokes(Map.Entry.class, "getKey", "getValue", "setValue")
		.addFieldRenamedInvoke("key", Map.Entry.class, "getKey")
		.addFieldGetterSetter(type(Map.Entry.class), "value", MAP_ENTRY_GET, MAP_ENTRY_SET)
		.addType("SortedMap", SortedMap.class)
		.addMethodInvokes(SortedMap.class, "firstKey", "lastKey")
		.addType("NavigableMap", NavigableMap.class)
		.addMethodMultiInvokes(NavigableMap.class, "lowerEntry", "lowerKey", "floorEntry", "floorKey", "ceilingEntry", "ceilingKey", "higherEntry", "higherKey", "firstEntry", "lastEntry", "pollFirstEntry", "pollLastEntry", "descendingMap", "navigableKeySet", "descendingKeySet", "subMap", "headMap", "tailMap")
		.addType("TreeMap", TreeMap.class)
		.addQualifiedSpecificConstructor(TreeMap.class, SortedMap.class)
		.addQualifiedSpecificConstructor(TreeMap.class, Map.class)
		.addQualifiedSpecificConstructor(TreeMap.class)
		.addType("HashMap", HashMap.class)
		.addQualifiedMultiConstructor(HashMap.class)
		.addType("LinkedHashMap", LinkedHashMap.class)
		.addQualifiedMultiConstructor(LinkedHashMap.class)
		.addType("ConstantMap", ConstantMap.class)
		.addMemberKeyword(TypeInfos.CLASS, "new", new MemberKeywordHandler.Named("ConstantMap.new(key1: value1, key2: value2, ...)", (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			if (receiver.getConstantValue().isConstant() && receiver.getConstantValue().asJavaObject().equals(type(ConstantMap.class))) {
				return ldc(CONSTANT_MAP, inflate(ConstantMapSyntax.parse(parser).keysAndValues()));
			}
			return null;
		}))
		.addType("Iterable", Iterable.class)
		.addMethodInvoke(Iterable.class, "iterator")
		.addType("Collection", Collection.class)
		.addMethodInvokes(Collection.class, "size", "isEmpty", "contains", "add", "containsAll", "addAll", "removeAll", "retainAll", "clear")
		.addMethodRenamedInvoke("removeElement", Collection.class, "remove")
		.addFieldInvokes(Collection.class, "size", "isEmpty")
		.addType("Set", Set.class)
		.addType("SortedSet", SortedSet.class)
		.addMethodInvokes(SortedSet.class, "subSet", "headSet", "tailSet", "first", "last")
		.addType("NavigableSet", NavigableSet.class)
		.addMethodMultiInvokes(NavigableSet.class, "lower", "floor", "ceiling", "higher", "pollFirst", "pollLast", "descendingSet", "descendingIterator", "subSet", "headSet", "tailSet")
		.addType("TreeSet", TreeSet.class)
		.addQualifiedSpecificConstructor(TreeSet.class, SortedSet.class)
		.addQualifiedSpecificConstructor(TreeSet.class, Collection.class)
		.addQualifiedSpecificConstructor(TreeSet.class)
		.addType("HashSet", HashSet.class)
		.addQualifiedSpecificConstructor(HashSet.class)
		.addQualifiedSpecificConstructor(HashSet.class, int.class)
		.addQualifiedSpecificConstructor(HashSet.class, Collection.class)
		.addQualifiedSpecificConstructor(HashSet.class, int.class, float.class)
		.addType("LinkedHashSet", LinkedHashSet.class)
		.addQualifiedSpecificConstructor(LinkedHashSet.class)
		.addQualifiedSpecificConstructor(LinkedHashSet.class, int.class)
		.addQualifiedSpecificConstructor(LinkedHashSet.class, Collection.class)
		.addQualifiedSpecificConstructor(LinkedHashSet.class, int.class, float.class)
		.addType("ConstantSet", ConstantSet.class)
		.addQualifiedFunction(type(ConstantSet.class), "new", (ExpressionParser parser, String name, InsnTree... arguments) -> {
			int elementCount = arguments.length;
			ConstantValue[] constants = new ConstantValue[elementCount];
			for (int index = 0; index < elementCount; index++) {
				if (!(constants[index] = arguments[index].getConstantValue()).isConstantOrDynamic()) {
					throw new ScriptParsingException("Argument " + index + " is not a constant value: " + arguments[index].describe(), parser.input);
				}
			}
			return new CastResult(ldc(CONSTANT_SET, inflate(constants)), false);
		})
		.addType("List", List.class)
		.addMethodMultiInvokes(List.class, "addAll", "add", "get", "set", "indexOf", "lastIndexOf", "listIterator", "subList")
		.addMethodMultiInvokeStatic(JavaUtilScriptEnvironment.class, "shuffle")
		.addMethodInvokeStatic(Collections.class, "reverse")
		.addMethodRenamedInvokeSpecific("removeIndex", List.class, "remove", Object.class, int.class)
		.addMethod(TypeInfo.of(List.class), "", new Named("List.(index)", (parser, receiver, name, mode, arguments) -> {
			InsnTree index = ScriptEnvironment.castArgument(parser, "", TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(
				NormalListMapGetterInsnTree.from(receiver, LIST_GET, index, LIST_SET, "List", mode),
				index != arguments[0]
			);
		}))
		.addType("LinkedList", LinkedList.class)
		.addQualifiedMultiConstructor(LinkedList.class)
		.addType("ArrayList", ArrayList.class)
		.addQualifiedMultiConstructor(ArrayList.class)
		.addType("ConstantList", ArrayWrapper.class)
		.addQualifiedFunction(type(ArrayWrapper.class), "new", (ExpressionParser parser, String name, InsnTree... arguments) -> {
			int argumentCount = arguments.length;
			ConstantValue[] constants = new ConstantValue[argumentCount];
			for (int index = 0; index < argumentCount; index++) {
				if (!(constants[index] = arguments[index].getConstantValue()).isConstantOrDynamic()) {
					throw new ScriptParsingException("Argument " + index + " is not a constant value: " + arguments[index].describe(), parser.input);
				}
			}
			return new CastResult(ldc(CONSTANT_LIST, inflate(constants)), false);
		})
		.addMethodInvokes(ArrayList.class, "trimToSize", "ensureCapacity")
		.addType("Queue", Queue.class)
		.addMethodInvokes(Queue.class, "offer", "remove", "poll", "element", "peek")
		.addType("Deque", Deque.class)
		.addMethodInvokes(Deque.class, "addFirst", "addLast", "offerFirst", "offerLast", "removeFirst", "removeLast", "pollFirst", "pollLast", "getFirst", "getLast", "peekFirst", "peekLast", "removeFirstOccurrence", "removeLastOccurrence", "push", "pop")
		.addType("ArrayDeque", ArrayDeque.class)
		.addQualifiedMultiConstructor(ArrayDeque.class)
		.addType("PriorityQueue", PriorityQueue.class)
		.addQualifiedMultiConstructor(PriorityQueue.class)
		.addType("RandomList", IRandomList.class)
		.addMethodMultiInvokes(IRandomList.class, "getWeight", "setWeight", "add", "set", "iterator", "listIterator", "subList")
		.addMethodInvokeSpecific(IRandomList.class, "getRandomElement", Object.class, RandomGenerator.class)
		.addType("RandomArrayList", RandomList.class)
		.addQualifiedMultiConstructor(RandomList.class)
	);

	public static Consumer<MutableScriptEnvironment> withoutRandom() {
		return (MutableScriptEnvironment environment) -> environment.addAll(ALL);
	}

	public static Consumer<MutableScriptEnvironment> withRandom(InsnTree loadRandom) {
		return (MutableScriptEnvironment environment) -> {
			environment
			.addAll(ALL)
			.addMethod(
				type(List.class),
				"shuffle",
				Handlers
				.builder(JavaUtilScriptEnvironment.class, "shuffle")
				.addReceiverArgument(List.class)
				.addImplicitArgument(loadRandom)
				.buildMethod()
			);
		};
	}

	public static void swap(Object[] array, int index1, int index2) {
		Object tmp = array[index1];
		array[index1] = array[index2];
		array[index2] = tmp;
	}

	/**
	mostly a copy-paste of {@link Collections#shuffle(List, Random)},
	but adapted to work with a {@link RandomGenerator} instead of a {@link Random}.
	*/
	public static <T> void shuffle(List<T> list, RandomGenerator random) {
		int size = list.size();
		if (size < 5 || list instanceof RandomAccess) {
			for (int index = size; index > 1; index--) {
				Collections.swap(list, index - 1, random.nextInt(index));
			}
		}
		else {
			@SuppressWarnings({ "unchecked", "SuspiciousArrayCast" })
			T[] array = (T[])(list.toArray());
			for (int index = size; index > 1; index--) {
				swap(array, index - 1, random.nextInt(index));
			}
			ListIterator<T> iterator = list.listIterator();
			for (T element : array) {
				iterator.next();
				iterator.set(element);
			}
		}
	}

	public static <T> void shuffle(List<T> list, long seed) {
		int size = list.size();
		if (size < 5 || list instanceof RandomAccess) {
			for (int index = size; index > 1; index--) {
				Collections.swap(list, index - 1, Permuter.nextBoundedInt(seed += Permuter.PHI64, index));
			}
		}
		else {
			@SuppressWarnings({ "unchecked", "SuspiciousArrayCast" })
			T[] array = (T[])(list.toArray());
			for (int index = size; index > 1; index--) {
				swap(array, index - 1, Permuter.nextBoundedInt(seed += Permuter.PHI64, index));
			}
			ListIterator<T> iterator = list.listIterator();
			for (T element : array) {
				iterator.next();
				iterator.set(element);
			}
		}
	}

	public static <K, V> void setEntryValue(Map.Entry<K, V> entry, V value) {
		entry.setValue(value);
	}

	public static ArrayWrapper<Object> constantList(MethodHandles.Lookup caller, String name, Class<?> type, Object... contents) {
		return new ArrayWrapper<>(deflate(contents));
	}

	public static ConstantMap<Object, Object> constantMap(MethodHandles.Lookup caller, String name, Class<?> type, Object... arguments) {
		return new ConstantMap<>(deflate(arguments));
	}

	public static ConstantSet<Object> constantSet(MethodHandles.Lookup caller, String name, Class<?> type, Object... args) {
		return new ConstantSet<>(deflate(args));
	}

	public static ConstantValue[] inflate(ConstantValue[] args) {
		int length = args.length;
		ConstantValue[] result = new ConstantValue[(length * (Long.SIZE / 3 + 1) + (Long.SIZE / 3 - 1)) / (Long.SIZE / 3)];
		int writeIndex = 0;
		for (int baseIndex = 0; baseIndex < length; baseIndex += Long.SIZE / 3) {
			long types = 0L;
			int typeIndex = writeIndex++;
			for (int offset = 0; offset < Long.SIZE / 3; offset++) {
				int index = baseIndex + offset;
				if (index >= length) {
					types >>>= 3;
				}
				else {
					int type = inflateOne(args[index]);
					result[writeIndex++] = args[index];
					types = (types >>> 3) | (((long)(type)) << (Long.SIZE - 4));
				}
			}
			result[typeIndex] = constant(types);
		}
		assert writeIndex == result.length;
		return result;
	}

	public static Object[] deflate(Object[] args) {
		int length = args.length;
		int writeIndex = 0;
		outer:
		for (int baseIndex = 0; baseIndex < length; baseIndex += Long.SIZE / 3 + 1) {
			long types = (Long)(args[baseIndex]);
			for (int offset = 1; offset < Long.SIZE / 3 + 1; offset++) {
				int index = baseIndex + offset;
				if (index >= length) break outer;
				args[writeIndex++] = deflateOne(((int)(types)) & 7, args[index]);
				types >>>= 3;
			}
		}
		return Arrays.copyOf(args, writeIndex);
	}

	public static int inflateOne(ConstantValue value) {
		return switch (value.getTypeInfo().getSort()) {
			case BOOLEAN -> 1;
			case BYTE -> 2;
			case SHORT -> 3;
			case CHAR -> 4;
			case OBJECT, ARRAY -> value instanceof NullConstantValue ? 5 : 0;
			default -> 0;
		};
	}

	public static Object deflateOne(int type, Object value) {
		return switch (type) {
			case 0 -> value;
			case 1 -> Boolean.valueOf(((Integer)(value)).intValue() != 0);
			case 2 -> Byte.valueOf(((Integer)(value)).byteValue());
			case 3 -> Short.valueOf(((Integer)(value)).shortValue());
			case 4 -> Character.valueOf((char)(((Integer)(value)).intValue()));
			case 5 -> null;
			default -> throw new IllegalArgumentException(value + " cannot be cast with type " + type);
		};
	}
}