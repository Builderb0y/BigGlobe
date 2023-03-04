package builderb0y.scripting.environments;

import java.util.*;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.InvokeInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class JavaUtilScriptEnvironment extends MultiScriptEnvironment {

	public static final MutableScriptEnvironment
		OBJECT = (
			new MutableScriptEnvironment()
			.addType("Object", Object.class)
			.addMethodInvokes(Object.class, "toString", "equals", "hashCode", "getClass")
		),
		ITERATOR = (
			new MutableScriptEnvironment()
			.addType("Iterator", Iterator.class)
			.addMethodInvokes(Iterator.class, "hasNext", "next", "remove")
		),
		LIST_ITERATOR = (
			new MutableScriptEnvironment()
			.addType("ListIterator", ListIterator.class)
			.addMethodInvokes(ListIterator.class, "hasPrevious", "previous", "nextIndex", "previousIndex", "set", "add")
		),
		ITERATORS = (
			new MutableScriptEnvironment()
			.multiAddAll(ITERATOR, LIST_ITERATOR)
		),
		MAP = (
			new MutableScriptEnvironment()
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
			new MutableScriptEnvironment()
			.addType("SortedMap", SortedMap.class)
			.addMethodInvokes(SortedMap.class, "firstKey", "lastKey")
		),
		NAVIGABLE_MAP = (
			new MutableScriptEnvironment()
			.addType("NavigableMap", NavigableMap.class)
			.addMethodMultiInvokes(NavigableMap.class, "lowerEntry", "lowerKey", "floorEntry", "floorKey", "ceilingEntry", "ceilingKey", "higherEntry", "higherKey", "firstEntry", "lastEntry", "pollFirstEntry", "pollLastEntry", "descendingMap", "navigableKeySet", "descendingKeySet", "subMap", "headMap", "tailMap")
		),
		TREE_MAP = (
			new MutableScriptEnvironment()
			.addType("TreeMap", TreeMap.class)
			.addQualifiedSpecificConstructor(TreeMap.class, SortedMap.class)
			.addQualifiedSpecificConstructor(TreeMap.class, Map.class)
			.addQualifiedSpecificConstructor(TreeMap.class)
		),
		HASH_MAP = (
			new MutableScriptEnvironment()
			.addType("HashMap", HashMap.class)
			.addQualifiedMultiConstructor(HashMap.class)
		),
		LINKED_HASH_MAP = (
			new MutableScriptEnvironment()
			.addType("LinkedHashMap", LinkedHashMap.class)
			.addQualifiedMultiConstructor(LinkedHashMap.class)
		),
		MAPS = (
			new MutableScriptEnvironment()
			.multiAddAll(MAP, SORTED_MAP, NAVIGABLE_MAP, TREE_MAP, HASH_MAP, LINKED_HASH_MAP)
		),
		ITERABLE = (
			new MutableScriptEnvironment()
			.addType("Iterable", Iterable.class)
			.addMethodInvoke(Iterable.class, "iterator")
		),
		COLLECTION = (
			new MutableScriptEnvironment()
			.addType("Collection", Collection.class)
			.addMethodInvokes(Collection.class, "size", "isEmpty", "contains", "add", "containsAll", "addAll", "removeAll", "retainAll", "clear")
			.addMethodRenamedInvoke("removeElement", Collection.class, "remove")
		),
		SET = (
			new MutableScriptEnvironment()
			.addType("Set", Set.class)
		),
		SORTED_SET = (
			new MutableScriptEnvironment()
			.addType("SortedSet", SortedSet.class)
			.addMethodInvokes(SortedSet.class, "subSet", "headSet", "tailSet", "first", "last")
		),
		NAVIGABLE_SET = (
			new MutableScriptEnvironment()
			.addType("NavigableSet", NavigableSet.class)
			.addMethodMultiInvokes(NavigableSet.class, "lower", "floor", "ceiling", "higher", "pollFirst", "pollLast", "descendingSet", "descendingIterator", "subSet", "headSet", "tailSet")
		),
		TREE_SET = (
			new MutableScriptEnvironment()
			.addType("TreeSet", TreeSet.class)
			.addQualifiedSpecificConstructor(TreeSet.class, SortedSet.class)
			.addQualifiedSpecificConstructor(TreeSet.class, Collection.class)
			.addQualifiedSpecificConstructor(TreeSet.class)
		),
		HASH_SET = (
			new MutableScriptEnvironment()
			.addType("HashSet", HashSet.class)
			.addQualifiedSpecificConstructor(HashSet.class)
			.addQualifiedSpecificConstructor(HashSet.class, int.class)
			.addQualifiedSpecificConstructor(HashSet.class, Collection.class)
			.addQualifiedSpecificConstructor(HashSet.class, int.class, float.class)
		),
		LINKED_HASH_SET = (
			new MutableScriptEnvironment()
			.addType("LinkedHashSet", LinkedHashSet.class)
			.addQualifiedSpecificConstructor(LinkedHashSet.class)
			.addQualifiedSpecificConstructor(LinkedHashSet.class, int.class)
			.addQualifiedSpecificConstructor(LinkedHashSet.class, Collection.class)
			.addQualifiedSpecificConstructor(LinkedHashSet.class, int.class, float.class)
		),
		SETS = (
			new MutableScriptEnvironment()
			.multiAddAll(SET, SORTED_SET, NAVIGABLE_SET, TREE_SET, HASH_SET, LINKED_HASH_SET)
		),
		LIST = (
			new MutableScriptEnvironment()
			.addType("List", List.class)
			.addMethodMultiInvokes(List.class, "addAll", "add", "get", "set", "indexOf", "lastIndexOf", "listIterator", "subList")
			.addMethodRenamedInvokeSpecific("removeIndex", List.class, "remove", Object.class, int.class)
			.addMethod(TypeInfo.of(List.class), "", (parser, receiver, name, arguments) -> {
				InsnTree index = ScriptEnvironment.castArgument(parser, "", TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
				return new ListGetInsnTree(receiver, index);
			})
		),
		LINKED_LIST = (
			new MutableScriptEnvironment()
			.addType("LinkedList", LinkedList.class)
			.addQualifiedMultiConstructor(LinkedList.class)
		),
		ARRAY_LIST = (
			new MutableScriptEnvironment()
			.addType("ArrayList", ArrayList.class)
			.addQualifiedMultiConstructor(ArrayList.class)
			.addMethodInvokes(ArrayList.class, "trimToSize", "ensureCapacity")
		),
		LISTS = (
			new MutableScriptEnvironment()
			.multiAddAll(LIST, LINKED_LIST, ARRAY_LIST)
		),
		QUEUE = (
			new MutableScriptEnvironment()
			.addType("Queue", Queue.class)
			.addMethodInvokes(Queue.class, "offer", "remove", "poll", "element", "peek")
		),
		DEQUE = (
			new MutableScriptEnvironment()
			.addType("Deque", Deque.class)
			.addMethodInvokes(Deque.class, "addFirst", "addLast", "offerFirst", "offerLast", "removeFirst", "removeLast", "pollFirst", "pollLast", "getFirst", "getLast", "peekFirst", "peekLast", "removeFirstOccurrence", "removeLastOccurrence", "push", "pop")
		),
		ARRAY_DEQUE = (
			new MutableScriptEnvironment()
			.addType("ArrayDeque", ArrayDeque.class)
			.addQualifiedMultiConstructor(ArrayDeque.class)
		),
		PRIORITY_QUEUE = (
			new MutableScriptEnvironment()
			.addType("PriorityQueue", PriorityQueue.class)
			.addQualifiedMultiConstructor(PriorityQueue.class)
		),
		QUEUES = (
			new MutableScriptEnvironment()
			.multiAddAll(QUEUE, DEQUE, ARRAY_DEQUE, PRIORITY_QUEUE)
		),
		ITERABLES = (
			new MutableScriptEnvironment()
			.multiAddAll(ITERABLE, COLLECTION, SETS, LISTS, QUEUES)
		),
		ALL = (
			new MutableScriptEnvironment()
			.multiAddAll(OBJECT, MAPS, ITERABLES, ITERATORS)
		);

	public static class MapGetInsnTree extends InvokeInsnTree {

		public static final MethodInfo
			GET = MethodInfo.getMethod(Map.class, "get"),
			PUT = MethodInfo.getMethod(Map.class, "put");

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
			GET = MethodInfo.getMethod(List.class, "get"),
			SET = MethodInfo.getMethod(List.class, "set");

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