package builderb0y.scripting.environments;

import java.util.*;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.collections.NormalListMapGetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class JavaUtilScriptEnvironment {

	public static final MethodInfo
		MAP_GET       = MethodInfo.getMethod(Map      .class, "get"),
		MAP_PUT       = MethodInfo.getMethod(Map      .class, "put"),
		MAP_ENTRY_GET = MethodInfo.getMethod(Map.Entry.class, "getValue"),
		MAP_ENTRY_SET = MethodInfo.getMethod(JavaUtilScriptEnvironment.class, "setEntryValue"),
		LIST_GET      = MethodInfo.getMethod(List     .class, "get"),
		LIST_SET      = MethodInfo.getMethod(List     .class, "set");

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
		.addMethod(TypeInfo.of(Map.class), "", (parser, receiver, name, mode, arguments) -> {
			InsnTree key = ScriptEnvironment.castArgument(parser, "", TypeInfos.OBJECT, CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(
				NormalListMapGetterInsnTree.from(receiver, MAP_GET, key, MAP_PUT, "Map", mode),
				key != arguments[0]
			);
		})
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
		.addType("List", List.class)
		.addMethodMultiInvokes(List.class, "addAll", "add", "get", "set", "indexOf", "lastIndexOf", "listIterator", "subList")
		.addMethodMultiInvokeStatic(JavaUtilScriptEnvironment.class, "shuffle")
		.addMethodInvokeStatic(Collections.class, "reverse")
		.addMethodRenamedInvokeSpecific("removeIndex", List.class, "remove", Object.class, int.class)
		.addMethod(TypeInfo.of(List.class), "", (parser, receiver, name, mode, arguments) -> {
			InsnTree index = ScriptEnvironment.castArgument(parser, "", TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(
				NormalListMapGetterInsnTree.from(receiver, LIST_GET, index, LIST_SET, "List", mode),
				index != arguments[0]
			);
		})
		.addType("LinkedList", LinkedList.class)
		.addQualifiedMultiConstructor(LinkedList.class)
		.addType("ArrayList", ArrayList.class)
		.addQualifiedMultiConstructor(ArrayList.class)
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
}