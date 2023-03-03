package builderb0y.scripting.environments;

import java.util.*;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.InvokeInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class JavaUtilScriptEnvironment extends MultiScriptEnvironment {

	public static final MutableScriptEnvironment2
		OBJECT = (
			new MutableScriptEnvironment2()
			.addType("Object", Object.class)
			.addMethodInvokes(Object.class, "toString", "equals", "hashCode", "getClass")
		),
		ITERATOR = (
			new MutableScriptEnvironment2()
			.addType("Iterator", Iterator.class)
			.addMethodInvokes(Iterator.class, "hasNext", "next", "remove")
		),
		LIST_ITERATOR = (
			new MutableScriptEnvironment2()
			.addType("ListIterator", ListIterator.class)
			.addMethodInvokes(ListIterator.class, "hasPrevious", "previous", "nextIndex", "previousIndex", "set", "add")
		),
		ITERATORS = (
			new MutableScriptEnvironment2()
			.multiAddAll(ITERATOR, LIST_ITERATOR)
		),
		MAP = (
			new MutableScriptEnvironment2()
			.addType("Map", Map.class)
			.addType("MapEntry", Map.Entry.class)
			.addMethodMultiInvokes(Map.class, "size", "isEmpty", "containsKey", "containsValue", "get", "put", "remove", "putAll", "clear", "keySet", "values", "entrySet", "getOrDefault", "putIfAbsent", "replace")
			.addMethodInvokes(Map.Entry.class, "getKey", "getValue", "setValue")
			.addMethod(TypeInfo.of(Map.class), "", (parser, receiver, name, arguments) -> {
				InsnTree key = ScriptEnvironment.castArgument(parser, "", TypeInfos.OBJECT, CastMode.IMPLICIT_THROW, arguments);
				return new MapGetInsnTree(receiver, key);
			})
		),
		SORTED_MAP = (
			new MutableScriptEnvironment2()
			.addType("SortedMap", SortedMap.class)
			.addMethodInvokes(SortedMap.class, "firstKey", "lastKey")
		),
		NAVIGABLE_MAP = (
			new MutableScriptEnvironment2()
			.addType("NavigableMap", NavigableMap.class)
			.addMethodMultiInvokes(NavigableMap.class, "lowerEntry", "lowerKey", "floorEntry", "floorKey", "ceilingEntry", "ceilingKey", "higherEntry", "higherKey", "firstEntry", "lastEntry", "pollFirstEntry", "pollLastEntry", "descendingMap", "navigableKeySet", "descendingKeySet", "subMap", "headMap", "tailMap")
		),
		TREE_MAP = (
			new MutableScriptEnvironment2()
			.addType("TreeMap", TreeMap.class)
			.addQualifiedSpecificConstructor(TreeMap.class, SortedMap.class)
			.addQualifiedSpecificConstructor(TreeMap.class, Map.class)
			.addQualifiedSpecificConstructor(TreeMap.class)
		),
		HASH_MAP = (
			new MutableScriptEnvironment2()
			.addType("HashMap", HashMap.class)
			.addQualifiedMultiConstructor(HashMap.class)
		),
		LINKED_HASH_MAP = (
			new MutableScriptEnvironment2()
			.addType("LinkedHashMap", LinkedHashMap.class)
			.addQualifiedMultiConstructor(LinkedHashMap.class)
		),
		MAPS = (
			new MutableScriptEnvironment2()
			.multiAddAll(MAP, SORTED_MAP, NAVIGABLE_MAP, TREE_MAP, HASH_MAP, LINKED_HASH_MAP)
		),
		ITERABLE = (
			new MutableScriptEnvironment2()
			.addType("Iterable", Iterable.class)
			.addMethodInvoke(Iterable.class, "iterator")
		),
		COLLECTION = (
			new MutableScriptEnvironment2()
			.addType("Collection", Collection.class)
			.addMethodInvokes(Collection.class, "size", "isEmpty", "contains", "add", "remove", "containsAll", "addAll", "removeAll", "retainAll", "clear")
		),
		SET = (
			new MutableScriptEnvironment2()
			.addType("Set", Set.class)
		),
		SORTED_SET = (
			new MutableScriptEnvironment2()
			.addType("SortedSet", SortedSet.class)
			.addMethodInvokes(SortedSet.class, "subSet", "headSet", "tailSet", "first", "last")
		),
		NAVIGABLE_SET = (
			new MutableScriptEnvironment2()
			.addType("NavigableSet", NavigableSet.class)
			.addMethodMultiInvokes(NavigableSet.class, "lower", "floor", "ceiling", "higher", "pollFirst", "pollLast", "descendingSet", "descendingIterator", "subSet", "headSet", "tailSet")
		),
		TREE_SET = (
			new MutableScriptEnvironment2()
			.addType("TreeSet", TreeSet.class)
			.addQualifiedSpecificConstructor(TreeSet.class, SortedSet.class)
			.addQualifiedSpecificConstructor(TreeSet.class, Collection.class)
			.addQualifiedSpecificConstructor(TreeSet.class)
		),
		HASH_SET = (
			new MutableScriptEnvironment2()
			.addType("HashSet", HashSet.class)
			.addQualifiedSpecificConstructor(HashSet.class)
			.addQualifiedSpecificConstructor(HashSet.class, int.class)
			.addQualifiedSpecificConstructor(HashSet.class, Collection.class)
			.addQualifiedSpecificConstructor(HashSet.class, int.class, float.class)
		),
		LINKED_HASH_SET = (
			new MutableScriptEnvironment2()
			.addType("LinkedHashSet", LinkedHashSet.class)
			.addQualifiedSpecificConstructor(LinkedHashSet.class)
			.addQualifiedSpecificConstructor(LinkedHashSet.class, int.class)
			.addQualifiedSpecificConstructor(LinkedHashSet.class, Collection.class)
			.addQualifiedSpecificConstructor(LinkedHashSet.class, int.class, float.class)
		),
		SETS = (
			new MutableScriptEnvironment2()
			.multiAddAll(SET, SORTED_SET, NAVIGABLE_SET, TREE_SET, HASH_SET, LINKED_HASH_SET)
		),
		LIST = (
			new MutableScriptEnvironment2()
			.addType("List", List.class)
			.addMethodMultiInvokes(List.class, "addAll", "add", "remove", "get", "set", "indexOf", "lastIndexOf", "listIterator", "subList")
			.addMethod(TypeInfo.of(List.class), "", (parser, receiver, name, arguments) -> {
				InsnTree index = ScriptEnvironment.castArgument(parser, "", TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
				return new ListGetInsnTree(receiver, index);
			})
		),
		LINKED_LIST = (
			new MutableScriptEnvironment2()
			.addType("LinkedList", LinkedList.class)
			.addQualifiedMultiConstructor(LinkedList.class)
		),
		ARRAY_LIST = (
			new MutableScriptEnvironment2()
			.addType("ArrayList", ArrayList.class)
			.addQualifiedMultiConstructor(ArrayList.class)
			.addMethodInvokes(ArrayList.class, "trimToSize", "ensureCapacity")
		),
		LISTS = (
			new MutableScriptEnvironment2()
			.multiAddAll(LIST, LINKED_LIST, ARRAY_LIST)
		),
		QUEUE = (
			new MutableScriptEnvironment2()
			.addType("Queue", Queue.class)
			.addMethodInvokes(Queue.class, "offer", "remove", "poll", "element", "peek")
		),
		DEQUE = (
			new MutableScriptEnvironment2()
			.addType("Deque", Deque.class)
			.addMethodInvokes(Deque.class, "addFirst", "addLast", "offerFirst", "offerLast", "removeFirst", "removeLast", "pollFirst", "pollLast", "getFirst", "getLast", "peekFirst", "peekLast", "removeFirstOccurrence", "removeLastOccurrence", "push", "pop")
		),
		ARRAY_DEQUE = (
			new MutableScriptEnvironment2()
			.addType("ArrayDeque", ArrayDeque.class)
			.addQualifiedMultiConstructor(ArrayDeque.class)
		),
		PRIORITY_QUEUE = (
			new MutableScriptEnvironment2()
			.addType("PriorityQueue", PriorityQueue.class)
			.addQualifiedMultiConstructor(PriorityQueue.class)
		),
		QUEUES = (
			new MutableScriptEnvironment2()
			.multiAddAll(QUEUE, DEQUE, ARRAY_DEQUE, PRIORITY_QUEUE)
		),
		ITERABLES = (
			new MutableScriptEnvironment2()
			.multiAddAll(ITERABLE, COLLECTION, SETS, LISTS, QUEUES)
		),
		ALL = (
			new MutableScriptEnvironment2()
			.multiAddAll(MAPS, ITERABLES, ITERATORS)
		);

	public static class MapGetInsnTree extends InvokeInsnTree {

		public static final MethodInfo
			GET = MethodInfo.forMethod(ReflectionData.forClass(Map.class).getDeclaredMethod("get")),
			PUT = MethodInfo.forMethod(ReflectionData.forClass(Map.class).getDeclaredMethod("put"));

		public MapGetInsnTree(InsnTree map, InsnTree key) {
			super(INVOKEINTERFACE, map, GET, key);
		}

		@Override
		public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
			if (op == UpdateOp.ASSIGN) {
				return invokeInterface(this.receiver, PUT, this.args[0], rightValue.cast(parser, TypeInfos.OBJECT, CastMode.IMPLICIT_THROW)).cast(parser, TypeInfos.VOID, CastMode.EXPLICIT_THROW);
			}
			throw new ScriptParsingException("Updating Map not yet implemented", parser.input);
		}
	}

	public static class ListGetInsnTree extends InvokeInsnTree {

		public static final MethodInfo
			GET = MethodInfo.forMethod(ReflectionData.forClass(List.class).getDeclaredMethod("get")),
			SET = MethodInfo.forMethod(ReflectionData.forClass(List.class).getDeclaredMethod("set"));

		public ListGetInsnTree(InsnTree list, InsnTree index) {
			super(INVOKEINTERFACE, list, GET, index);
		}

		@Override
		public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
			if (op == UpdateOp.ASSIGN) {
				return invokeInterface(this.receiver, SET, this.args[0], rightValue.cast(parser, TypeInfos.OBJECT, CastMode.IMPLICIT_THROW)).cast(parser, TypeInfos.VOID, CastMode.EXPLICIT_THROW);
			}
			throw new ScriptParsingException("Updating List not yet implemented", parser.input);
		}
	}
}