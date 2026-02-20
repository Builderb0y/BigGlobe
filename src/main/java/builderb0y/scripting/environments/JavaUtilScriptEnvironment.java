package builderb0y.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.scripting.wrappers.ArrayWrapper;
import builderb0y.bigglobe.scripting.wrappers.ConstantMap;
import builderb0y.bigglobe.scripting.wrappers.ConstantSet;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.ConstantValue.NullConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.collections.NormalListMapGetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MemberKeywordHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler.Named;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.special.ConstantMapSyntax;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class JavaUtilScriptEnvironment {

	public static final MethodInfo
		MAP_GET             = MethodInfo.getMethod(Map      .class, "get"),
		MAP_PUT             = MethodInfo.getMethod(Map      .class, "put"),
		MAP_ENTRY_GET_KEY   = MethodInfo.getMethod(Map.Entry.class, "getKey"),
		MAP_ENTRY_GET_VALUE = MethodInfo.getMethod(Map.Entry.class, "getValue"),
		MAP_ENTRY_SET_VALUE = MethodInfo.inCaller("setEntryValue"),
		LIST_GET            = MethodInfo.getMethod(List     .class, "get"),
		LIST_SET            = MethodInfo.getMethod(List     .class, "set"),
		CONSTANT_LIST       = MethodInfo.inCaller("constantList"),
		CONSTANT_MAP        = MethodInfo.inCaller("constantMap"),
		CONSTANT_SET        = MethodInfo.inCaller("constantSet");

	@Deprecated //use noAllocateNoModify() instead.
	public static final MutableScriptEnvironment NO_ALLOCATE_NO_MODIFY = (
		new MutableScriptEnvironment()
		.addType("ArrayDeque",      ArrayDeque   .class)
		.addType("ArrayList",       ArrayList    .class)
		.addType("Collection",      Collection   .class)
		.addType("ConstantList",    ArrayWrapper .class)
		.addType("ConstantMap",     ConstantMap  .class)
		.addType("ConstantSet",     ConstantSet  .class)
		.addType("Deque",           Deque        .class)
		.addType("HashMap",         HashMap      .class)
		.addType("HashSet",         HashSet      .class)
		.addType("Iterable",        Iterable     .class)
		.addType("Iterator",        Iterator     .class)
		.addType("LinkedHashMap",   LinkedHashMap.class)
		.addType("LinkedHashSet",   LinkedHashSet.class)
		.addType("LinkedList",      LinkedList   .class)
		.addType("List",            List         .class)
		.addType("ListIterator",    ListIterator .class)
		.addType("Map",             Map          .class)
		.addType("MapEntry",        Map.Entry    .class)
		.addType("NavigableMap",    NavigableMap .class)
		.addType("NavigableSet",    NavigableSet .class)
		.addType("PriorityQueue",   PriorityQueue.class)
		.addType("Queue",           Queue        .class)
		.addType("RandomArrayList", RandomList   .class)
		.addType("RandomList",      IRandomList  .class)
		.addType("Set",             Set          .class)
		.addType("SortedMap",       SortedMap    .class)
		.addType("SortedSet",       SortedSet    .class)
		.addType("TreeMap",         TreeMap      .class)
		.addType("TreeSet",         TreeSet      .class)

		.addFieldInvokes(Map.class, "size", "isEmpty")
		.addFieldInvoke("key", MAP_ENTRY_GET_KEY)
		.addFieldInvokes(Collection.class, "size", "isEmpty")

		.addMethodInvokes(Object.class, "toString", "equals", "hashCode", "getClass")
		.addMethodInvokes(Iterator.class, "hasNext", "next")
		.addMethodInvokes(ListIterator.class, "hasPrevious", "previous", "nextIndex", "previousIndex")
		.addMethodMultiInvokes(Map.class, "size", "isEmpty", "containsKey", "containsValue", "get", "keySet", "values", "entrySet", "getOrDefault")
		.addMethodInvokes(Map.Entry.class, "getKey", "getValue")
		.addMethodInvokes(SortedMap.class, "firstKey", "lastKey", "subMap", "headMap", "tailMap")
		.addMethodMultiInvokes(NavigableMap.class, "lowerEntry", "lowerKey", "floorEntry", "floorKey", "ceilingEntry", "ceilingKey", "higherEntry", "higherKey", "firstEntry", "lastEntry", "descendingMap", "navigableKeySet", "descendingKeySet", "subMap", "headMap", "tailMap")
		.addMethodInvoke(Iterable.class, "iterator")
		.addMethodInvokes(Collection.class, "size", "isEmpty", "contains", "containsAll")
		.addMethodInvokes(SortedSet.class, "subSet", "headSet", "tailSet", "first", "last")
		.addMethodMultiInvokes(NavigableSet.class, "lower", "floor", "ceiling", "higher", "descendingSet", "descendingIterator", "subSet", "headSet", "tailSet")
		.addMethodMultiInvokes(List.class, "get", "indexOf", "lastIndexOf", "listIterator", "subList")
		.addMethodInvokes(Queue.class, "element", "peek")
		.addMethodInvokes(Deque.class, "getFirst", "getLast", "peekFirst", "peekLast")
		.addMethodMultiInvokes(IRandomList.class, "getWeight", "iterator", "listIterator", "subList")
		.addMethodInvokeSpecific(IRandomList.class, "getRandomIndex", int.class, RandomGenerator.class)
		.addMethodInvokeSpecific(IRandomList.class, "getRandomIndex", int.class, long.class)
		.addMethodInvokeSpecific(IRandomList.class, "getRandomElement", Object.class, RandomGenerator.class)
		.addMethodInvokeSpecific(IRandomList.class, "getRandomElement", Object.class, long.class)

		.addMemberKeyword(TypeInfos.CLASS, "new", new MemberKeywordHandler.Named("ConstantMap.new(key1: value1, key2: value2, ...)", (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			if (receiver.getConstantValue().isConstant() && receiver.getConstantValue().asJavaObject().equals(type(ConstantMap.class))) {
				return ldc(CONSTANT_MAP, inflate(ConstantMapSyntax.parse(parser).keysAndValues()));
			}
			return null;
		}))
		.addQualifiedFunction(type(ConstantSet.class), "new", new FunctionHandler.Named("ConstantSet.new(values)", (ExpressionParser parser, String name, InsnTree... arguments) -> {
			int elementCount = arguments.length;
			ConstantValue[] constants = new ConstantValue[elementCount];
			for (int index = 0; index < elementCount; index++) {
				if (!(constants[index] = arguments[index].getConstantValue()).isConstantOrDynamic()) {
					throw new ScriptParsingException("Argument " + index + " is not a constant value: " + arguments[index].describe(), parser.input);
				}
			}
			return new CastResult(ldc(CONSTANT_SET, inflate(constants)), false);
		}))
		.addQualifiedFunction(type(ArrayWrapper.class), "new", new FunctionHandler.Named("ConstantList.new(values)", (ExpressionParser parser, String name, InsnTree... arguments) -> {
			int argumentCount = arguments.length;
			ConstantValue[] constants = new ConstantValue[argumentCount];
			for (int index = 0; index < argumentCount; index++) {
				if (!(constants[index] = arguments[index].getConstantValue()).isConstantOrDynamic()) {
					throw new ScriptParsingException("Argument " + index + " is not a constant value: " + arguments[index].describe(), parser.input);
				}
			}
			return new CastResult(ldc(CONSTANT_LIST, inflate(constants)), false);
		}))
	);

	public static Consumer<MutableScriptEnvironment> noAllocateNoModify() {
		return (MutableScriptEnvironment environment) -> {
			environment
			.addAll(NO_ALLOCATE_NO_MODIFY)
			.addMethodInvoke("", MAP_GET)
			.addMethodInvoke("", LIST_GET)
			.addFieldInvoke("value", MAP_ENTRY_GET_VALUE)
			;
		};
	}

	@Deprecated //use withRandom() or withoutRandom() instead.
	public static final MutableScriptEnvironment ALL = (
		new MutableScriptEnvironment()
		.addAll(NO_ALLOCATE_NO_MODIFY)
		.addMethodInvokes(Iterator.class, "remove")
		.addMethodInvokes(ListIterator.class, "set", "add")
		.addMethodMultiInvokes(Map.class, "put", "remove", "putAll", "clear", "putIfAbsent", "replace")
		.addMethod(type(Map.class), "", new Named("Map.(key)", (parser, receiver, name, mode, arguments) -> {
			InsnTree key = ScriptEnvironment.castArgument(parser, "", TypeInfos.OBJECT, CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(
				NormalListMapGetterInsnTree.from(receiver, MAP_GET, key, MAP_PUT, "Map", mode),
				key != arguments[0]
			);
		}))
		.addMethodInvokes(Map.Entry.class, "setValue")
		.addFieldGetterSetter(type(Map.Entry.class), "value", MAP_ENTRY_GET_VALUE, MAP_ENTRY_SET_VALUE)
		.addMethodMultiInvokes(NavigableMap.class, "pollFirstEntry", "pollLastEntry")
		.addQualifiedSpecificConstructor(TreeMap.class, SortedMap.class)
		.addQualifiedSpecificConstructor(TreeMap.class, Map.class)
		.addQualifiedSpecificConstructor(TreeMap.class)
		.addQualifiedMultiConstructor(HashMap.class)
		.addQualifiedMultiConstructor(LinkedHashMap.class)
		.addMethodInvokes(Collection.class, "add", "addAll", "removeAll", "retainAll", "clear")
		.addMethodRenamedInvoke("removeElement", Collection.class, "remove")
		.addMethodMultiInvokes(NavigableSet.class, "pollFirst", "pollLast")
		.addQualifiedSpecificConstructor(TreeSet.class, SortedSet.class)
		.addQualifiedSpecificConstructor(TreeSet.class, Collection.class)
		.addQualifiedSpecificConstructor(TreeSet.class)
		.addQualifiedSpecificConstructor(HashSet.class)
		.addQualifiedSpecificConstructor(HashSet.class, int.class)
		.addQualifiedSpecificConstructor(HashSet.class, Collection.class)
		.addQualifiedSpecificConstructor(HashSet.class, int.class, float.class)
		.addQualifiedSpecificConstructor(LinkedHashSet.class)
		.addQualifiedSpecificConstructor(LinkedHashSet.class, int.class)
		.addQualifiedSpecificConstructor(LinkedHashSet.class, Collection.class)
		.addQualifiedSpecificConstructor(LinkedHashSet.class, int.class, float.class)
		.addMethodMultiInvokes(List.class, "add", "set")
		.addMethodMultiInvokeStatic(JavaUtilScriptEnvironment.class, "shuffle")
		.addMethodInvokeStatic(Collections.class, "reverse")
		.addMethodRenamedInvokeSpecific("removeIndex", List.class, "remove", Object.class, int.class)
		.addMethod(type(List.class), "", new Named("List.(index)", (parser, receiver, name, mode, arguments) -> {
			InsnTree index = ScriptEnvironment.castArgument(parser, "", TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(
				NormalListMapGetterInsnTree.from(receiver, LIST_GET, index, LIST_SET, "List", mode),
				index != arguments[0]
			);
		}))
		.addQualifiedMultiConstructor(LinkedList.class)
		.addQualifiedMultiConstructor(ArrayList.class)
		.addMethodInvokes(ArrayList.class, "trimToSize", "ensureCapacity")
		.addMethodInvokes(Queue.class, "offer", "remove", "poll")
		.addMethodInvokes(Deque.class, "addFirst", "addLast", "offerFirst", "offerLast", "removeFirst", "removeLast", "pollFirst", "pollLast", "removeFirstOccurrence", "removeLastOccurrence", "push", "pop")
		.addQualifiedMultiConstructor(ArrayDeque.class)
		.addQualifiedMultiConstructor(PriorityQueue.class)
		.addMethodMultiInvokes(IRandomList.class, "setWeight", "add", "set")
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
	an overload in Collections which uses a RandomGenerator was added in java 21.
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