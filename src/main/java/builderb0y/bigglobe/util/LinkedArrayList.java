package builderb0y.bigglobe.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
a frankenstein combo of LinkedList and ArrayList.
the primary storage method of this implementation is linked nodes.
however, any time random access is requested,
it will generate an array cache containing all the nodes in order.
subsequent random access calls will then use this array.
therefore, the first random access call will run in O(N) time.
however, all subsequent random access calls will run in O(1) time.
the array cache is invalidated any time the size of the list changes.

since it's impossible to predict when users will create/invalidate the array cache,
this class does NOT implement RandomAccess.

a secondary benefit over LinkedList is that LinkedArrayList grants direct access to its internal node structure.
the nodes and the links between nodes can be freely modified.
additionally, several convenience methods are available for manipulating nodes or ranges of nodes safely.
obviously, if you do plan on modifying node links directly, you should do so with caution.

since the nodes and elements can both be manipulated,
it does not make sense for this class to directly implement List<T> or List<Node<T>>.
it is also impossible to implement both of these interfaces simultaneously.
instead, methods are provided to return a *view* of the nodes and elements independently.
these views implement both List and Deque.
additionally, changes in the LinkedArrayList will be reflected in these views and vise versa.

cloning this list will return an independent list which contains the same elements,
but will not be affected by changes to this list or vise versa.
cloning a list will NOT clone the elements it contains.
*/
@SuppressWarnings({ "unchecked", "unused" })
public class LinkedArrayList<T> implements Cloneable {

	public static boolean ASSERTS = false;

	/** the first and last node in this list, or null if the list is empty. */
	public @Nullable Node<T> first, last;
	/** the number of nodes in this list. */
	public int size;
	/**
	all nodes in this list in array form.
	this field is set to null whenever the list is modified, and is re-computed again when needed.
	for example, getNode/Element(i, true) will initialize this field if it's currently null.
	*/
	public @Nullable Node<T>[] arrayCache;

	//initialized on first use
	public @Nullable NodeList nodeList;
	public @Nullable ElementList elementList;

	public LinkedArrayList() {}

	//////////////////////////////// Factory methods ////////////////////////////////

	/**
	returns a new empty LinkedArrayList.
	this list can be modified freely, and is not immutable.
	*/
	public static <T> LinkedArrayList<T> empty() {
		return new LinkedArrayList<>();
	}

	/** creates a new LinkedArrayList containing only this node. */
	public static <T> LinkedArrayList<T> ofNode(Node<T> node) {
		LinkedArrayList<T> list = new LinkedArrayList<>();
		list.first = list.last = node.initListIndex(0);
		list.size = 1;
		if (ASSERTS) list.checkLinks();
		return list;
	}

	/** creates a new LinkedArrayList containing only this element. */
	public static <T> LinkedArrayList<T> ofElement(T element) {
		return ofNode(new Node<>(element));
	}

	/**
	creates a new LinkedArrayList containing all the nodes in the provided array.
	the nodes are automatically linked together.
	as such, none of the nodes should be part of any other list prior to this method call.
	additionally, the array cache of the returned list is initialized to the provided node array.
	*/
	@SafeVarargs
	public static <T> LinkedArrayList<T> ofNodes(Node<T>... nodes) {
		LinkedArrayList<T> list = new LinkedArrayList<>();
		int length = nodes.length;
		if (length == 0) return list;
		list.size  = length;
		list.first = nodes[0].initListIndex(0);
		list.last  = nodes[--length];
		list.arrayCache = nodes;
		for (int i = 0; i < length;) {
			link(nodes[i++], nodes[i].initListIndex(i));
		}
		if (ASSERTS) list.checkLinks();
		return list;
	}

	/**
	creates a new LinkedArrayList containing all the nodes in the provided collection.
	the nodes are automatically linked together.
	as such, none of the nodes should be part of any other list prior to this method call.
	unlike ofNodes(Node<T>...), this method does NOT initialize the list's array cache.
	*/
	public static <T> LinkedArrayList<T> ofNodes(Collection<? extends Node<T>> nodes) {
		LinkedArrayList<T> list = new LinkedArrayList<>();
		int size = nodes.size();
		if (size == 0) return list;
		list.size = size;
		Iterator<? extends Node<T>> iterator = nodes.iterator();
		Node<T> current = list.first = iterator.next().initListIndex(0);
		for (int i = 1; i < size; i++) {
			link(current, current = iterator.next().initListIndex(i));
		}
		list.last = current;
		if (ASSERTS) list.checkLinks();
		return list;
	}

	/** creates a new LinkedArrayList containing all the elements in the provided array. */
	@SafeVarargs
	public static <T> LinkedArrayList<T> ofElements(T... elements) {
		LinkedArrayList<T> list = new LinkedArrayList<>();
		int length = elements.length;
		if (length == 0) return list;
		list.size = length;
		Node<T> current = list.first = new Node<>(elements[0]).initListIndex(0);
		for (int i = 1; i < length; i++) {
			link(current, current = new Node<>(elements[i]).initListIndex(i));
		}
		list.last = current;
		if (ASSERTS) list.checkLinks();
		return list;
	}

	/** returns a new LinkedArrayList containing all the elements in the provided collection. */
	public static <T> LinkedArrayList<T> ofElements(Collection<? extends T> elements) {
		LinkedArrayList<T> list = new LinkedArrayList<>();
		int size = elements.size();
		if (size == 0) return list;
		list.size = size;
		Iterator<? extends T> iterator = elements.iterator();
		Node<T> current = list.first = new Node<>(iterator.next());
		for (int i = 1; i < size; i++) {
			link(current, current = new Node<T>(iterator.next()).initListIndex(i));
		}
		list.last = current;
		if (ASSERTS) list.checkLinks();
		return list;
	}

	/**
	creates a new LinkedArrayList containing all the nodes from fromHere to toHere.
	the links of these nodes are NOT modified.
	as such, all of the links should be provided by the caller.
	*/
	public static <T> LinkedArrayList<T> ofNodeRange(Node<T> fromHere, Node<T> toHere, int count) {
		LinkedArrayList<T> list = new LinkedArrayList<>();
		list.first = fromHere;
		list.last = toHere;
		list.size = count;
		if (ASSERTS) list.checkLinks();
		return list;
	}

	//////////////////////////////// Query methods ////////////////////////////////

	/** returns the number of nodes in this list. */
	public int size() {
		return this.size;
	}

	/** returns true if there are no nodes in this list, false otherwise. */
	public boolean isEmpty() {
		return this.size == 0;
	}

	/** returns true if there are any nodes in this list, false otherwise. */
	public boolean isNotEmpty() {
		return this.size != 0;
	}

	public @NotNull Node<T> getFirstNode() {
		Node<T> first;
		if ((first = this.first) != null) return first;
		else throw new NoSuchElementException();
	}

	public @Nullable Node<T> peekFirstNode() {
		return this.first;
	}

	public T getFirstElement() {
		return this.getFirstNode().element;
	}

	public @Nullable T peekFirstElement() {
		Node<T> first;
		return (first = this.first) != null ? first.element : null;
	}

	public @NotNull Node<T> getLastNode() {
		Node<T> last;
		if ((last = this.last) != null) return last;
		else throw new NoSuchElementException();
	}

	public @Nullable Node<T> peekLastNode() {
		return this.last;
	}

	public T getLastElement() {
		return this.getLastNode().element;
	}

	public @Nullable T peekLastElement() {
		Node<T> last;
		return (last = this.last) != null ? last.element : null;
	}

	/**
	returns the node at the specified index.
	if the index is out of bounds, an IndexOutOfBoundsException is thrown.
	*/
	public Node<T> getNode(int index, boolean cache) {
		if (index < 0 || index >= this.size()) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size());
		}
		Node<T>[] array = this.getArrayCache(cache);
		if (array != null) return array[index];
		Node<T> node;
		if (index < this.size() >> 1) {
			node = this.getFirstNode();
			for (int i = 0; i < index; i++) node = node.getNext();
		}
		else {
			node = this.getLastNode();
			for (int i = this.size(); --i > index;) node = node.getPrev();
		}
		return node;
	}

	public T getElement(int index, boolean cache) {
		return this.getNode(index, cache).element;
	}

	/**
	returns the position of the node in this list.
	a return value of {@link Node#NOT_IN_LIST} indicates
	that the specified node is not part of this list.
	if the array cache exists, then this method will run in O(1) time.
	since the array cannot contain duplicate nodes,
	this method also doubles as an implementation of lastIndexOfNode().
	*/
	public int indexOfNode(Node<?> node, boolean cache) {
		if (this.isEmpty()) return Node.NOT_IN_LIST;
		//check if it's in ANY list first.
		if (!node.isInList()) return Node.NOT_IN_LIST;
		//at this point, the node is guaranteed to be in some list, but not necessarily this one.
		Node<T>[] array = this.getArrayCache(cache);
		if (array != null) {
			//if it is in our list and arrayCache exists, the node's index will be valid.
			//final test: if its index points to its own location in the array, then the node is indeed part of this list.
			int index = node.index;
			return index >= 0 && index < array.length && array[index] == node ? index : Node.NOT_IN_LIST;
		}
		else {
			//even though the node's index is not guaranteed to be correct,
			//it will still serve as a "better than random" guess as to its true position.
			//we use this guess to pick the direction in which we search for it.
			if (node.index < (this.size() >> 1)) {
				int index = 0; //start searching from the beginning of this list
				for (Node<T> test = this.first; test != null; test = test.next, index++) {
					if (test == node) return index;
				}
			}
			else {
				int index = this.size() - 1; //start searching from the end of this list
				for (Node<T> test = this.last; test != null; test = test.prev, index--) {
					if (test == node) return index;
				}
			}
			return Node.NOT_IN_LIST;
		}
	}

	/**
	returns the first node (starting at index) whose element equals the specified element.
	as a bonus, the returned node's index is guaranteed to be correct.
	*/
	public @Nullable Node<T> getFirstNodeFor(Object element, int index) {
		if (index < 0) index = 0;
		else if (index >= this.size()) return null;

		final Node<T>[] arrayCache = this.getArrayCache(false);
		if (arrayCache != null) {
			//skip node index initialization, since that's guaranteed to be correct when the array cache exists.
			//as a side bonus, array traversal might be slightly faster with CPU caching effects.
			//use arrayCache.length instead of this.size() to increase the chance that JIT will skip bounds checking for these array accesses.
			final int length = arrayCache.length;
			if (element != null) {
				while (index < length) {
					if (element.equals(arrayCache[index].element)) return arrayCache[index];
					index++;
				}
			}
			else {
				while (index < length) {
					if (arrayCache[index].element == null) return arrayCache[index];
					index++;
				}
			}
		}
		else {
			if (element != null) {
				for (Node<T> node = this.getNode(index, false); node != null; node = node.next, index++) {
					if (element.equals(node.element)) return node;
				}
			}
			else {
				for (Node<T> node = this.getNode(index, false); node != null; node = node.next, index++) {
					if (node.element == null) return node;
				}
			}
		}
		return null;
	}

	/**
	returns the first node (ending at index) whose element equals the specified element.
	as a bonus, the returned node's index is guaranteed to be correct.
	*/
	public @Nullable Node<T> getLastNodeFor(Object element, int index) {
		if (index < 0) return null;
		else if (index >= this.size()) index = this.size() - 1;

		final Node<T>[] arrayCache = this.getArrayCache(false);
		if (arrayCache != null) {
			//skip node index initialization, since that's guaranteed to be correct when the array cache exists.
			//as a side bonus, array traversal might be slightly faster with CPU caching effects.
			if (element != null) {
				while (index >= 0) {
					if (element.equals(arrayCache[index].element)) return arrayCache[index];
					index--;
				}
			}
			else {
				while (index >= 0) {
					if (arrayCache[index].element == null) return arrayCache[index];
					index--;
				}
			}
		}
		else {
			if (element != null) {
				for (Node<T> node = this.getNode(index, false); node != null; node = node.prev) {
					if (element.equals(node.element)) return node;
					index--;
				}
			}
			else {
				for (Node<T> node = this.getNode(index, false); node != null; node = node.prev) {
					if (node.element == null) return node;
					index--;
				}
			}
		}
		return null;
	}

	/** returns the index of the first node which contains element. */
	public int indexOfElement(Object element, int index) {
		Node<T> node = this.getFirstNodeFor(element, index);
		return node != null ? node.index : Node.NOT_IN_LIST;
	}

	/** returns the index of the last node which contains element. */
	public int lastIndexOfElement(Object element, int index) {
		Node<T> node = this.getLastNodeFor(element, index);
		return node != null ? node.index : Node.NOT_IN_LIST;
	}

	/** returns true if the specified node is part of this list, false otherwise. */
	public boolean containsNode(Node<?> node, boolean cache) {
		return this.indexOfNode(node, cache) != Node.NOT_IN_LIST;
	}

	/** returns all nodes in this list as an array. */
	@Contract("true -> !null")
	public Node<T>[] getArrayCache(boolean create) {
		if (this.arrayCache == null) {
			if (this.isEmpty()) {
				this.arrayCache = Node.EMPTY_ARRAY;
			}
			else if (create) {
				this.arrayCache = this.toNodeArray(Node.EMPTY_ARRAY);
			}
		}
		if (ASSERTS) this.checkLinks();
		return this.arrayCache;
	}

	public <T1> T1[] toNodeArray(T1[] a) {
		int size = this.size();
		Object[] array = a.length >= size ? a : (Object[])(Array.newInstance(a.getClass().getComponentType(), size));
		Node<T>[] arrayCache = this.arrayCache;
		if (arrayCache != null) {
			//fast path
			System.arraycopy(arrayCache, 0, array, 0, size);
		}
		else {
			int index = 0;
			for (Node<T> node = this.first; node != null; node = node.next, index++) {
				array[node.index = index] = node;
			}
			assert index == size;
		}
		if (array.length > size) array[size] = null;
		if (ASSERTS) this.checkLinks();
		return (T1[])(array);
	}

	public <T1> T1[] toElementArray(T1[] a) {
		int size = this.size();
		Object[] array = a.length >= size ? a : (Object[])(Array.newInstance(a.getClass().getComponentType(), size));
		Node<T>[] arrayCache = this.arrayCache;
		if (arrayCache != null) {
			//skip node index initialization, since that's guaranteed to be correct when the array cache exists.
			//as a side bonus, array traversal might be slightly faster with CPU caching effects.
			for (int i = 0; i < size; i++) {
				array[i] = arrayCache[i].element;
			}
		}
		else {
			int index = 0;
			for (Node<T> node = this.first; node != null; node = node.next, index++) {
				array[node.index = index] = node.element;
			}
			assert index == size;
		}
		if (array.length > size) array[size] = null;
		if (ASSERTS) this.checkLinks();
		return (T1[])(array);
	}

	@Override
	public String toString() {
		return Arrays.toString(this.toElementArray(new Object[this.size()]));
	}

	public NodeList nodes() {
		return this.nodeList != null ? this.nodeList : (this.nodeList = this.new NodeList());
	}

	public ElementList elements() {
		return this.elementList != null ? this.elementList : (this.elementList = this.new ElementList());
	}

	@Override
	public LinkedArrayList<T> clone() {
		try {
			@SuppressWarnings("unchecked")
			LinkedArrayList<T> clone = (LinkedArrayList<T>)(super.clone());
			if (clone.isEmpty()) return clone;
			clone.arrayCache = null;
			clone.nodeList = null;
			clone.elementList = null;
			int index = 0;
			Node<T> lastOriginal = clone.getLastNode();
			Node<T> currentOriginal = clone.getFirstNode();
			Node<T> currentCopy = currentOriginal.clone();
			clone.first = currentCopy;
			while (currentOriginal != lastOriginal) {
				Node<T> nextOriginal = currentOriginal.getNext();
				Node<T> nextCopy = nextOriginal.clone();
				link(currentCopy, nextCopy);
				currentOriginal = nextOriginal;
				currentCopy = nextCopy;
				index++;
			}
			clone.last = currentCopy;
			if (ASSERTS) clone.checkLinks();
			return clone;
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError(e);
		}
	}

	//////////////////////////////// modification ////////////////////////////////

	//////////////// Adding nodes ////////////////

	/** adds toAdd to the beginning of this list. */
	public void addNodeToStart(Node<T> toAdd) {
		toAdd.initListIndex(0);
		this.arrayCache = null;
		Node<T> first = this.first;
		if (first == null) this.first = this.last = toAdd;
		else link(this.first = toAdd, first);
		this.size++;
		if (ASSERTS) this.checkLinks();
	}

	public void addElementToStart(T element) {
		this.addNodeToStart(new Node<>(element));
	}

	/** removes all the nodes from toAdd, and adds them to the start of this list instead. */
	public void addListToStart(LinkedArrayList<T> toAdd) {
		if (toAdd.isEmpty()) return;
		this.arrayCache = null;
		toAdd.arrayCache = null;
		Node<T> first = this.first;
		this.first = toAdd.first;
		if (first == null) this.last = toAdd.last;
		else link(toAdd.last, first);
		this.size += toAdd.size;
		toAdd.size = 0;
		toAdd.first = toAdd.last = null;
		if (ASSERTS) this.checkLinks();
		if (ASSERTS) toAdd.checkLinks();
	}

	/** adds toAdd to the end of this list. */
	public void addNodeToEnd(Node<T> toAdd) {
		toAdd.initListIndex(this.size());
		this.arrayCache = null;
		Node<T> last = this.last;
		if (last == null) this.first = this.last = toAdd;
		else link(last, this.last = toAdd);
		this.size++;
		if (ASSERTS) this.checkLinks();
	}

	public void addElementToEnd(T element) {
		this.addNodeToEnd(new Node<>(element));
	}

	/** removes all the nodes from toAdd, and adds them to the end of this list instead. */
	public void addListToEnd(LinkedArrayList<T> toAdd) {
		if (toAdd.isEmpty()) return;
		this.arrayCache = null;
		toAdd.arrayCache = null;
		Node<T> last = this.last;
		this.last = toAdd.last;
		if (last == null) this.first = toAdd.first;
		else link(last, toAdd.first);
		this.size += toAdd.size;
		toAdd.size = 0;
		toAdd.first = toAdd.last = null;
		if (ASSERTS) this.checkLinks();
		if (ASSERTS) toAdd.checkLinks();
	}

	/** inserts toInsert before existingNode in this list. */
	public void insertNodeBefore(Node<T> existingNode, Node<T> toInsert) {
		existingNode.ensureInList();
		if (existingNode == this.first) {
			this.addNodeToStart(toInsert);
		}
		else {
			toInsert.initListIndex(Math.max(existingNode.index - 1, 0));
			this.arrayCache = null;
			Node<T> prev = existingNode.getPrev();
			link(prev, toInsert);
			link(toInsert, existingNode);
			this.size++;
			if (ASSERTS) this.checkLinks();
		}
	}

	public void insertElementBefore(Node<T> existingNode, T toInsert) {
		this.insertNodeBefore(existingNode, new Node<>(toInsert));
	}

	/** removes all nodes from toInsert and inserts them before existingNode in this list. */
	public void insertListBefore(Node<T> existingNode, LinkedArrayList<T> toInsert) {
		if (existingNode == this.first) {
			this.addListToStart(toInsert);
		}
		else {
			if (toInsert.isEmpty()) return;
			this.arrayCache = null;
			toInsert.arrayCache = null;
			Node<T> prev = existingNode.getPrev();
			link(prev, toInsert.getFirstNode());
			link(toInsert.getLastNode(), existingNode);
			this.size += toInsert.size;
			toInsert.size = 0;
			toInsert.first = toInsert.last = null;
			if (ASSERTS) this.checkLinks();
			if (ASSERTS) toInsert.checkLinks();
		}
	}

	/** inserts toInsert after existingNode in this list. */
	public void insertNodeAfter(Node<T> existingNode, Node<T> toInsert) {
		existingNode.ensureInList();
		if (existingNode == this.last) {
			this.addNodeToEnd(toInsert);
		}
		else {
			toInsert.initListIndex(existingNode.index + 1);
			this.arrayCache = null;
			Node<T> next = existingNode.getNext();
			link(existingNode, toInsert);
			link(toInsert, next);
			this.size++;
			if (ASSERTS) this.checkLinks();
		}
	}

	public void insertElementAfter(Node<T> existingNode, T toInsert) {
		this.insertNodeAfter(existingNode, new Node<>(toInsert));
	}

	/** removes all nodes from toInsert and inserts them after existingNode in this list. */
	public void insertListAfter(Node<T> existingNode, LinkedArrayList<T> toInsert) {
		if (existingNode == this.last) {
			this.addListToEnd(toInsert);
		}
		else {
			if (toInsert.isEmpty()) return;
			this.arrayCache = null;
			toInsert.arrayCache = null;
			Node<T> next = existingNode.getNext();
			link(existingNode, toInsert.getFirstNode());
			link(toInsert.getLastNode(), next);
			this.size += toInsert.size;
			toInsert.size = 0;
			toInsert.first = toInsert.last = null;
			if (ASSERTS) this.checkLinks();
			if (ASSERTS) toInsert.checkLinks();
		}
	}

	//////////////// Removing nodes ////////////////

	public Node<T> removeFirstNode() {
		Node<T> node = this.getFirstNode();
		link(null, this.first = node.next);
		if (--this.size == 0) this.last = null;
		node.removedFromList();
		if (ASSERTS) this.checkLinks();
		return node;
	}

	public T removeFirstElement() {
		return this.removeFirstNode().element;
	}

	public Node<T> removeLastNode() {
		Node<T> node = this.getLastNode();
		link(this.last = node.prev, null);
		if (--this.size == 0) this.first = null;
		node.removedFromList();
		if (ASSERTS) this.checkLinks();
		return node;
	}

	public T removeLastElement() {
		return this.removeLastNode().element;
	}

	/** removes toRemove from this list. */
	public void removeNode(Node<T> toRemove) {
		toRemove.ensureInList();
		this.arrayCache = null;
		Node<T> prev = toRemove.prev, next = toRemove.next;
		this.size--;
		link(prev, next);
		if (toRemove == this.first) this.first = next;
		if (toRemove == this.last) this.last = prev;
		toRemove.removedFromList();
		if (ASSERTS) this.checkLinks();
	}

	/** removes all nodes between and including fromHere and toHere from this list. */
	public void removeNodeRange(Node<T> fromHere, Node<T> toHere) {
		fromHere.ensureInList();
		toHere.ensureInList();
		this.arrayCache = null;
		Node<T> prev = fromHere.prev, next = toHere.next;
		this.size -= countAndRemove(fromHere, toHere);
		link(prev, next);
		if (fromHere == this.first) this.first = next;
		if (toHere == this.last) this.last = prev;
		if (ASSERTS) this.checkLinks();
	}

	//////////////// Replacing nodes ////////////////

	/** removes replaceThis from this list and inserts withThis where replaceThis used to be. */
	public void replaceNode(Node<T> replaceThis, Node<T> withThis) {
		replaceThis.ensureInList();
		if (this.arrayCache != null) {
			assert this.arrayCache[replaceThis.index] == replaceThis;
			this.arrayCache[replaceThis.index] = withThis.initListIndex(replaceThis.index);
		}
		else {
			withThis.initListIndex(replaceThis.index);
		}
		Node<T> prev = replaceThis.prev, next = replaceThis.next;
		replaceThis.removedFromList();
		link(prev, withThis);
		link(withThis, next);
		if (replaceThis == this.first) this.first = withThis;
		if (replaceThis == this.last ) this.last  = withThis;
		if (ASSERTS) this.checkLinks();
	}

	/**
	removes replaceThis from this list and inserts all
	nodes in withThis where replaceThis used to be.
	withThis is cleared in the process.
	*/
	public void replaceNode(Node<T> replaceThis, LinkedArrayList<T> withThis) {
		switch (withThis.size()) {
			case 0 -> this.removeNode(replaceThis);
			case 1 -> this.replaceNode(replaceThis, withThis.removeFirstNode());
			default -> {
				this.arrayCache = null;
				withThis.arrayCache = null;
				Node<T> prev = replaceThis.prev, next = replaceThis.next;
				link(prev, withThis.first);
				link(withThis.last, next);
				if (replaceThis == this.first) this.first = withThis.first;
				if (replaceThis == this.last) this.last = withThis.last;
				replaceThis.removedFromList();
				this.size += withThis.size() - 1;
				withThis.size = 0;
				withThis.first = withThis.last = null;
				if (ASSERTS) this.checkLinks();
			}
		}
		if (ASSERTS) withThis.checkLinks();
	}

	/** removes all the nodes between and including fromHere and toHere and inserts all nodes in withThis in their place. */
	public void replaceNodeRange(Node<T> fromHere, Node<T> toHere, Node<T> withThis) {
		if (fromHere == toHere) {
			this.replaceNode(fromHere, withThis);
			return;
		}
		fromHere.ensureInList();
		toHere.ensureInList();
		withThis.initListIndex(fromHere.index);
		this.arrayCache = null;
		Node<T> prev = fromHere.prev, next = toHere.next;
		this.size += 1 - countAndRemove(fromHere, toHere);
		if (fromHere == this.first) this.first = withThis;
		if (toHere == this.last) this.last = withThis;
		link(prev, withThis);
		link(withThis, next);
		if (ASSERTS) this.checkLinks();
	}

	/**
	removes all nodes between and including fromHere and toHere
	from this list and inserts all nodes in withThis in their place.
	withThis is cleared in the process.
	*/
	public void replaceNodeRange(Node<T> fromHere, Node<T> toHere, LinkedArrayList<T> withThis) {
		switch (withThis.size()) {
			case 0 -> this.removeNodeRange(fromHere, toHere);
			case 1 -> this.replaceNodeRange(fromHere, toHere, withThis.removeFirstNode());
			default -> {
				this.arrayCache = null;
				withThis.arrayCache = null;
				Node<T> prev = fromHere.prev, next = toHere.next;
				this.size += withThis.size() - countAndRemove(fromHere, toHere);
				link(prev, withThis.first);
				link(withThis.last, next);
				if (fromHere == this.first) this.first = withThis.first;
				if (toHere == this.last) this.last = withThis.last;
				withThis.size = 0;
				withThis.first = withThis.last = null;
				if (ASSERTS) this.checkLinks();
			}
		}
		if (ASSERTS) withThis.checkLinks();
	}

	//////////////// Other operations ////////////////

	/**
	removes all nodes between and including fromHere and toHere from this list,
	and returns a new list which contains them in the same order.
	*/
	public LinkedArrayList<T> slice(Node<T> fromHere, Node<T> toHere) {
		int removed = count(fromHere, toHere);
		assert removed > 0 : removed;
		if (removed == 1) {
			assert fromHere == toHere;
			this.removeNode(fromHere);
			return ofNode(fromHere);
		}

		this.arrayCache = null;
		this.size -= removed;
		if (this.first == fromHere) this.first = toHere.next;
		if (this.last == toHere) this.last = fromHere.prev;
		link(fromHere.prev, toHere.next);
		if (ASSERTS) this.checkLinks();

		return ofNodeRange(fromHere, toHere, removed);
	}

	/**
	returns a new list which contains all the nodes between fromHere and toHere.
	the returned list will not be modified by any method call to this list, or vise versa.
	*/
	public LinkedArrayList<T> copyOfNodeRange(Node<T> fromHere, Node<T> toHere) {
		LinkedArrayList<T> copy = new LinkedArrayList<>();
		int index = 0;
		Node<T> currentOriginal = fromHere;
		Node<T> currentCopy = new Node<>(currentOriginal.element).initListIndex(0);
		copy.first = currentCopy;
		while (true) {
			index++;
			if (currentOriginal == toHere) break;
			Node<T> nextOriginal = currentOriginal.getNext();
			Node<T> nextCopy = new Node<>(nextOriginal.element).initListIndex(index);
			link(currentCopy, nextCopy);
			currentOriginal = nextOriginal;
			currentCopy = nextCopy;
		}
		copy.last = currentCopy;
		copy.size = index;
		if (ASSERTS) copy.checkLinks();
		return copy;
	}

	/**
	sorts the nodes in this LinkedArrayList in ascending order,
	according to the provided comparator.
	after this call returns, for each index in the list, {@code
		comparator.compare(list.getNode(index), list.getNode(index + 1))
	}
	will be strictly negative, assuming neither the
	nodes nor the comparator is modified during sorting.
	to sort in descending order instead, reverse the comparator
	before passing it into this method {@link Comparator#reversed()}.

	if the provided comparator is null, the nodes will be sorted according to their natural order.
	note however that at the time of writing this, {@link Node} does not
	implement {@link Comparable}, and thus does not have a natural order.
	as such, passing a null comparator into this method will fail
	with a {@link ClassCastException} unless the list is empty.
	or unless you're doing something very weird by subclassing {@link Node}.
	or mixing into it, I guess.

	to sort the *elements* in this list, use {@link #sortElements(Comparator)} instead.

	if the comparator throws an exception from its {@link Comparator#compare(Object, Object)}
	method, the list will be left in an undefined order,
	but will otherwise have a valid internal structure.
	*/
	public void sortNodes(@Nullable Comparator<? super Node<T>> comparator) {
		if (this.size() <= 1) return;
		//dump the nodes into an array and sort the array.
		Node<T>[] array = this.getArrayCache(true);
		try {
			Arrays.sort(array, comparator);
		}
		finally {
			//re-link the nodes, and update their indexes.
			int length = array.length;
			array[0].index = 0;
			(this.first = array[0]).prev = null;
			for (int index = 1; index < length; index++) {
				Node<T> current = array[index];
				//the following line is equivalent to:
				//	current.prev = array[index - 1];
				//	array[index - 1] (or current.prev) .next = current;
				//	current.index = index;
				//basically, it's an extremely compact way to
				//link the current node and the previous node,
				//while also initializing the current node's index.
				((current.prev = array[index - 1]).next = current).index = index;
			}
			(this.last = array[length - 1]).next = null;
			if (ASSERTS) this.checkLinks();
		}
	}

	/**
	sorts the elements in this LinkedArrayList in ascending order,
	according to the provided comparator.
	after this call returns, for each index in the list, {@code
		comparator.compare(list.getElement(index), list.getElement(index + 1))
	}
	will be strictly negative, assuming neither the
	elements nor the comparator is modified during sorting.
	to sort in descending order instead, reverse the comparator
	before passing it into this method {@link Comparator#reversed()}.

	if the provided comparator is null, the elements will be sorted according to their natural order.
	as such, if the elements do not implement {@link Comparable}, then a null
	comparator will fail with a {@link ClassCastException} unless the list is empty.

	to sort the *nodes* in this list, use {@link #sortNodes(Comparator)} instead.

	if the comparator throws an exception from its {@link Comparator#compare(Object, Object)}
	method, the list will be left in an undefined order,
	but will otherwise have a valid internal structure.
	*/
	public void sortElements(@Nullable Comparator<? super T> comparator) {
		this.sortNodes(Comparator.comparing(Node::getElement, comparator != null ? comparator : (Comparator<? super T>)(Comparator.naturalOrder())));
	}

	/** removes all the nodes from this list. */
	public void clear() {
		if (this.isEmpty()) return;
		countAndRemove(this.first, this.last);
		this.first = this.last = null;
		this.size = 0;
		this.arrayCache = null;
	}

	//////////////////////////////// Utility methods ////////////////////////////////

	//////////////// Iterators ////////////////

	public static <T> Iterator<Node<T>> nodeIteratorStartingAt(Node<T> start) {
		NodeIterator<T> iterator = new NodeIterator<>();
		iterator.next = start;
		return iterator;
	}

	public static <T> Iterator<Node<T>> nodeIteratorEndingAt(Node<T> end) {
		DescendingNodeIterator<T> iterator = new DescendingNodeIterator<>();
		iterator.prev = end;
		return iterator;
	}

	public static <T> Iterator<T> elementIteratorStartingAt(Node<T> start) {
		ElementIterator<T> iterator = new ElementIterator<>();
		iterator.next = start;
		return iterator;
	}

	public static <T> Iterator<T> elementIteratorEndingAt(Node<T> end) {
		DescendingElementIterator<T> iterator = new DescendingElementIterator<>();
		iterator.prev = end;
		return iterator;
	}

	//////////////// Spliterators ////////////////

	public static <T> Spliterator<Node<T>> nodeSpliteratorStartingAt(Node<T> start) {
		return Spliterators.spliteratorUnknownSize(nodeIteratorStartingAt(start), Spliterator.ORDERED);
	}

	public static <T> Spliterator<Node<T>> nodeSpliteratorEndingAt(Node<T> end) {
		return Spliterators.spliteratorUnknownSize(nodeIteratorEndingAt(end), Spliterator.ORDERED);
	}

	public static <T> Spliterator<T> elementSpliteratorStartingAt(Node<T> start) {
		return Spliterators.spliteratorUnknownSize(elementIteratorStartingAt(start), Spliterator.ORDERED);
	}

	public static <T> Spliterator<T> elementSpliteratorEndingAt(Node<T> end) {
		return Spliterators.spliteratorUnknownSize(elementIteratorEndingAt(end), Spliterator.ORDERED);
	}

	//////////////// Streams ////////////////

	public static <T> Stream<Node<T>> nodeStreamStartingAt(Node<T> start) {
		return StreamSupport.stream(nodeSpliteratorStartingAt(start), false);
	}

	public static <T> Stream<Node<T>> nodeStreamEndingAt(Node<T> end) {
		return StreamSupport.stream(nodeSpliteratorEndingAt(end), false);
	}

	public static <T> Stream<T> elementStreamStartingAt(Node<T> start) {
		return StreamSupport.stream(elementSpliteratorStartingAt(start), false);
	}

	public static <T> Stream<T> elementStreamEndingAt(Node<T> end) {
		return StreamSupport.stream(elementSpliteratorEndingAt(end), false);
	}

	public static <T> Collector<T, LinkedArrayList<T>, LinkedArrayList<T>> collector() {
		return Collector.of(
			LinkedArrayList::new,
			LinkedArrayList::addElementToEnd,
			(LinkedArrayList<T> list1, LinkedArrayList<T> list2) -> { list1.addListToEnd(list2); return list1; }
		);
	}

	//////////////////////////////// Internal methods ////////////////////////////////

	@SuppressWarnings({ "AssertWithSideEffects", "ConstantConditions" })
	public static void checkAssertsEnabled() {
		boolean asserts = false;
		assert asserts = true; //intentional side effect
		if (!asserts) throw new AssertionError("Asserts must be enabled in LinkedArrayList. Please add -ea to your JVM arguments.");
	}

	/**
	verifies that all nodes in this list are linked together correctly.
	if the arrayCache is present, will also verify that all nodes have the correct index.
	*/
	public void checkLinks() {
		checkAssertsEnabled();
		Node<T>[] arrayCache = this.arrayCache;
		if (arrayCache != null) {
			assert arrayCache.length == this.size() : "arrayCache is wrong length. Size: " + this.size() + ", Array: " + Arrays.toString(arrayCache);
		}
		if (this.isEmpty()) {
			assert this.first == null : "List is empty, but first is " + this.first;
			assert this.last  == null : "List is empty, but last is " + this.last;
		}
		else {
			Node<T> current = this.first;
			assert current != null : "No first entry";
			assert current.isInList() : current + " not in list";
			assert current.prev == null : "First entry had previous: " + current.prev;
			if (arrayCache != null) {
				assert current.index == 0 : "current.index should be 0, but it was " + current.index;
				assert arrayCache[0] == current : "current: " + current + ", array: " + arrayCache[0];
			}
			int actualSize = 1; //count first
			for (Node<T> next; (next = current.next) != null; current = next) {
				assert next.isInList() : next + " not in list";
				assert next.prev == current : "current: " + current + ", next: " + next + ", next.prev: " + next.prev;
				if (arrayCache != null) {
					assert next.index == actualSize : "next.index should be " + actualSize + " but it was " + current.index;
					assert arrayCache[actualSize] == next : "next: " + next + ", array: " + arrayCache[actualSize];
				}
				actualSize++;
			}
			assert this.size() == actualSize : "size: " + this.size() + ", actualSize: " + actualSize;
			assert current == this.last : "current: " + current + ", last: " + this.last;
		}
	}

	/** counts the number of nodes between and including start and end. */
	public static <T> int count(Node<T> start, Node<T> end) {
		int count = 1;
		for (Node<T> node = start; node != end; node = node.getNext()) {
			count++;
		}
		return count;
	}

	/**
	counts the number of nodes between and including start and end
	and un-links all nodes in this range.
	*/
	public static <T> int countAndRemove(Node<T> start, Node<T> end) {
		int count = 1;
		for (Node<T> node = start; node != end; node = node.removeAndGetNext()) {
			count++;
		}
		end.removedFromList();
		return count;
	}

	/** links the two nodes together, and zaps dangling links if they exist. */
	public static <T> void link(@Nullable Node<T> node1, @Nullable Node<T> node2) {
		if (node1 == node2) {
			if (node1 == null) return;
			else throw new IllegalArgumentException("Cannot link " + node1 + " to itself");
		}
		if (node1 != null && node1.next != node2) {
			if (node1.next != null) node1.next.prev = null;
			node1.next = node2;
		}
		if (node2 != null && node2.prev != node1) {
			if (node2.prev != null) node2.prev.next = null;
			node2.prev = node1;
		}
	}

	//////////////////////////////// Wrappers for java collections ////////////////////////////////

	public class NodeList extends AbstractList<Node<T>> implements Deque<Node<T>>, Cloneable {

		public NodeList reversed() {
			throw new UnsupportedOperationException("todo: support reversing.");
		}

		public final LinkedArrayList<T> backingList() {
			return LinkedArrayList.this;
		}

		@Override
		public void addFirst(Node<T> node) {
			LinkedArrayList.this.addNodeToStart(node);
		}

		@Override
		public void addLast(Node<T> node) {
			LinkedArrayList.this.addNodeToEnd(node);
		}

		@Override
		public boolean offerFirst(Node<T> node) {
			LinkedArrayList.this.addNodeToStart(node);
			return true;
		}

		@Override
		public boolean offerLast(Node<T> node) {
			LinkedArrayList.this.addNodeToEnd(node);
			return true;
		}

		@Override
		public Node<T> removeFirst() {
			return LinkedArrayList.this.removeFirstNode();
		}

		@Override
		public Node<T> removeLast() {
			return LinkedArrayList.this.removeLastNode();
		}

		@Nullable
		@Override
		public Node<T> pollFirst() {
			return this.isEmpty() ? null : LinkedArrayList.this.removeFirstNode();
		}

		@Nullable
		@Override
		public Node<T> pollLast() {
			return this.isEmpty() ? null : LinkedArrayList.this.removeLastNode();
		}

		@Override
		public Node<T> getFirst() {
			return LinkedArrayList.this.getFirstNode();
		}

		@Override
		public Node<T> getLast() {
			return LinkedArrayList.this.getLastNode();
		}

		@Override
		public Node<T> peekFirst() {
			return LinkedArrayList.this.first;
		}

		@Override
		public Node<T> peekLast() {
			return LinkedArrayList.this.last;
		}

		@Override
		public boolean removeFirstOccurrence(Object object) {
			if (object instanceof Node<?> && LinkedArrayList.this.containsNode((Node<?>)(object), false)) {
				LinkedArrayList.this.removeNode((Node<T>)(object));
				return true;
			}
			return false;
		}

		@Override
		public boolean removeLastOccurrence(Object object) {
			if (object instanceof Node<?> && LinkedArrayList.this.containsNode((Node<?>)(object), false)) {
				LinkedArrayList.this.removeNode((Node<T>)(object));
				return true;
			}
			return false;
		}

		@Override
		public boolean offer(Node<T> node) {
			return this.offerLast(node);
		}

		@Override
		public Node<T> remove() {
			return this.removeFirst();
		}

		@Override
		public Node<T> poll() {
			return this.pollFirst();
		}

		@Override
		public Node<T> element() {
			return this.getFirst();
		}

		@Override
		public Node<T> peek() {
			return this.peekFirst();
		}

		@Override
		public void push(Node<T> node) {
			this.addFirst(node);
		}

		@Override
		public Node<T> pop() {
			return this.removeFirst();
		}

		@NotNull
		@Override
		public Iterator<Node<T>> descendingIterator() {
			DescendingNodeIterator<T> iterator = new DescendingNodeIterator<>();
			iterator.prev = LinkedArrayList.this.last;
			return iterator;
		}

		@Override
		public int size() {
			return LinkedArrayList.this.size;
		}

		@Override
		public boolean isEmpty() {
			return LinkedArrayList.this.size == 0;
		}

		@Override
		public boolean contains(Object object) {
			return object instanceof Node<?> && LinkedArrayList.this.containsNode((Node<?>)(object), true);
		}

		@NotNull
		@Override
		public Iterator<Node<T>> iterator() {
			NodeIterator<T> iterator = new NodeIterator<>();
			iterator.next = LinkedArrayList.this.first;
			return iterator;
		}

		@Override
		public void forEach(Consumer<? super Node<T>> action) {
			for (Node<T> node = LinkedArrayList.this.first; node != null; node = node.next) {
				action.accept(node);
			}
		}

		@NotNull
		@Override
		public Object[] toArray() {
			return LinkedArrayList.this.toNodeArray(new Object[this.size()]);
		}

		@NotNull
		@Override
		public <T1> T1[] toArray(@NotNull T1[] array) {
			return LinkedArrayList.this.toNodeArray(array);
		}

		@Override
		public boolean add(Node<T> node) {
			LinkedArrayList.this.addNodeToEnd(node);
			return true;
		}

		@Override
		public boolean remove(Object object) {
			if (this.contains(object)) {
				LinkedArrayList.this.removeNode((Node<T>)(object));
				return true;
			}
			return false;
		}

		@Override
		public boolean containsAll(@NotNull Collection<?> collection) {
			for (Object element : collection) {
				if (!this.contains(element)) return false;
			}
			return true;
		}

		@Override
		public boolean addAll(@NotNull Collection<? extends Node<T>> collection) {
			if (collection.isEmpty()) return false;
			if (collection == this) throw new IllegalArgumentException("Cannot add list to itself");
			LinkedArrayList.this.addListToEnd(LinkedArrayList.ofNodes(collection.toArray(Node.EMPTY_ARRAY)));
			return true;
		}

		@Override
		public boolean addAll(int index, @NotNull Collection<? extends Node<T>> collection) {
			if (collection.isEmpty()) return false;
			if (collection == this) throw new IllegalArgumentException("Cannot add list to itself");
			LinkedArrayList.this.insertListBefore(LinkedArrayList.this.getNode(index, false), LinkedArrayList.ofNodes(collection.toArray(Node.EMPTY_ARRAY)));
			return true;
		}

		@Override
		public boolean removeAll(@NotNull Collection<?> collection) {
			if (collection.isEmpty()) return false;
			if (collection == this) {
				LinkedArrayList.this.clear();
				return true;
			}
			return this.removeIf(collection::contains);
		}

		@Override
		public boolean removeIf(Predicate<? super Node<T>> filter) {
			boolean removed = false;
			for (Node<T> node = LinkedArrayList.this.first; node != null;) {
				Node<T> next = node.next;
				if (filter.test(node)) {
					LinkedArrayList.this.removeNode(node);
					removed = true;
				}
				node = next;
			}
			return removed;
		}

		@Override
		public boolean retainAll(@NotNull Collection<?> collection) {
			if (collection == this || this.isEmpty()) return false;
			if (collection.isEmpty()) {
				this.clear();
				return true;
			}
			return this.removeIf((Node<T> node) -> !collection.contains(node));
		}

		@Override
		public void replaceAll(UnaryOperator<Node<T>> operator) {
			if (this.isEmpty()) return;
			LinkedArrayList.this.arrayCache = null;
			Node<T> currentOriginal = LinkedArrayList.this.getFirstNode();
			Node<T> lastOriginal = LinkedArrayList.this.getLastNode();
			Node<T> currentCopy = operator.apply(currentOriginal);
			LinkedArrayList.this.first = currentCopy;
			while (currentOriginal != lastOriginal) {
				Node<T> nextOriginal = currentOriginal.getNext();
				Node<T> nextCopy = operator.apply(nextOriginal);
				link(currentCopy, nextCopy);
				currentOriginal = nextOriginal;
				currentCopy = nextCopy;
			}
			LinkedArrayList.this.last = currentCopy;
			if (ASSERTS) LinkedArrayList.this.checkLinks();
		}

		@Override
		public void sort(Comparator<? super Node<T>> comparator) {
			LinkedArrayList.this.sortNodes(comparator);
		}

		@Override
		public void clear() {
			LinkedArrayList.this.clear();
		}

		@Override
		public Node<T> get(int index) {
			return LinkedArrayList.this.getNode(index, true);
		}

		@Override
		public Node<T> set(int index, Node<T> node) {
			Node<T> original = LinkedArrayList.this.getNode(index, true);
			LinkedArrayList.this.replaceNode(original, node);
			return original;
		}

		@Override
		public void add(int index, Node<T> node) {
			LinkedArrayList.this.insertNodeBefore(LinkedArrayList.this.getNode(index, false), node);
		}

		@Override
		public Node<T> remove(int index) {
			Node<T> node = LinkedArrayList.this.getNode(index, false);
			LinkedArrayList.this.removeNode(node);
			return node;
		}

		@Override
		public int indexOf(Object object) {
			return object instanceof Node<?> ? LinkedArrayList.this.indexOfNode((Node<?>)(object), true) : -1;
		}

		@Override
		public int lastIndexOf(Object object) {
			//any given node will NEVER appear in the list more than once.
			//so indexOfNode is equivalent to lastIndexOfNode.
			return object instanceof Node<?> ? LinkedArrayList.this.indexOfNode((Node<?>)(object), true) : -1;
		}

		@NotNull
		@Override
		public ListIterator<Node<T>> listIterator() {
			NodeListIterator iterator = new NodeListIterator();
			iterator.next = LinkedArrayList.this.first;
			return iterator;
		}

		@NotNull
		@Override
		public ListIterator<Node<T>> listIterator(int index) {
			Node<T> node = LinkedArrayList.this.getNode(index, false);
			NodeListIterator iterator = new NodeListIterator();
			iterator.next = node;
			iterator.prev = node.prev;
			return iterator;
		}

		@Override
		public Spliterator<Node<T>> spliterator() {
			int size = LinkedArrayList.this.size;
			if (size == 0) return Spliterators.emptySpliterator();
			Node<T>[] arrayCache = LinkedArrayList.this.arrayCache;
			if (arrayCache != null) return Spliterators.spliterator(arrayCache, Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
			return new NodeSpliterator<>(LinkedArrayList.this.getFirstNode(), size);
		}

		@Override
		public void removeRange(int fromIndex, int toIndex) {
			if (toIndex < fromIndex) throw new IndexOutOfBoundsException(toIndex + " < " + fromIndex);
			if (toIndex == fromIndex) return;
			LinkedArrayList.this.removeNodeRange(LinkedArrayList.this.getNode(fromIndex, false), LinkedArrayList.this.getNode(toIndex - 1, false));
		}

		@Override
		public NodeList clone() {
			return LinkedArrayList.this.clone().nodes();
		}
	}

	public class ElementList extends AbstractList<T> implements Deque<T>, Cloneable {

		public ElementList reversed() {
			throw new UnsupportedOperationException("todo: support reversing.");
		}

		public final LinkedArrayList<T> backingList() {
			return LinkedArrayList.this;
		}

		@Override
		public void addFirst(T element) {
			LinkedArrayList.this.addElementToStart(element);
		}

		@Override
		public void addLast(T element) {
			LinkedArrayList.this.addElementToEnd(element);
		}

		@Override
		public boolean offerFirst(T element) {
			LinkedArrayList.this.addElementToStart(element);
			return true;
		}

		@Override
		public boolean offerLast(T element) {
			LinkedArrayList.this.addElementToEnd(element);
			return true;
		}

		@Override
		public T removeFirst() {
			return LinkedArrayList.this.removeFirstElement();
		}

		@Override
		public T removeLast() {
			return LinkedArrayList.this.removeLastElement();
		}

		@Nullable
		@Override
		public T pollFirst() {
			return this.isEmpty() ? null : LinkedArrayList.this.removeFirstElement();
		}

		@Nullable
		@Override
		public T pollLast() {
			return this.isEmpty() ? null : LinkedArrayList.this.removeLastElement();
		}

		@Override
		public T getFirst() {
			return LinkedArrayList.this.getFirstElement();
		}

		@Override
		public T getLast() {
			return LinkedArrayList.this.getLastElement();
		}

		@Override
		public T peekFirst() {
			Node<T> node = LinkedArrayList.this.first;
			return node != null ? node.element : null;
		}

		@Override
		public T peekLast() {
			Node<T> node = LinkedArrayList.this.last;
			return node != null ? node.element : null;
		}

		@Override
		public boolean removeFirstOccurrence(Object object) {
			Node<T> node = LinkedArrayList.this.getFirstNodeFor(object, 0);
			if (node == null) return false;
			LinkedArrayList.this.removeNode(node);
			return true;
		}

		@Override
		public boolean removeLastOccurrence(Object object) {
			Node<T> node = LinkedArrayList.this.getLastNodeFor(object, this.size() - 1);
			if (node == null) return false;
			LinkedArrayList.this.removeNode(node);
			return true;
		}

		@Override
		public boolean offer(T element) {
			return this.offerLast(element);
		}

		@Override
		public T remove() {
			return this.removeFirst();
		}

		@Override
		public T poll() {
			return this.pollFirst();
		}

		@Override
		public T element() {
			return this.getFirst();
		}

		@Override
		public T peek() {
			return this.peekFirst();
		}

		@Override
		public void push(T element) {
			this.addFirst(element);
		}

		@Override
		public T pop() {
			return this.removeFirst();
		}

		@NotNull
		@Override
		public Iterator<T> descendingIterator() {
			DescendingElementIterator<T> iterator = new DescendingElementIterator<>();
			iterator.prev = LinkedArrayList.this.last;
			return iterator;
		}

		@Override
		public int size() {
			return LinkedArrayList.this.size;
		}

		@Override
		public boolean isEmpty() {
			return LinkedArrayList.this.size == 0;
		}

		@Override
		public boolean contains(Object object) {
			return LinkedArrayList.this.getFirstNodeFor(object, 0) != null;
		}

		@NotNull
		@Override
		public Iterator<T> iterator() {
			ElementIterator<T> iterator = new ElementIterator<>();
			iterator.next = LinkedArrayList.this.first;
			return iterator;
		}

		@Override
		public void forEach(Consumer<? super T> action) {
			for (Node<T> node = LinkedArrayList.this.first; node != null; node = node.next) {
				action.accept(node.element);
			}
		}

		@NotNull
		@Override
		public Object[] toArray() {
			return LinkedArrayList.this.toElementArray(new Object[this.size()]);
		}

		@NotNull
		@Override
		public <T1> T1[] toArray(@NotNull T1[] array) {
			return LinkedArrayList.this.toElementArray(array);
		}

		@Override
		public boolean add(T element) {
			LinkedArrayList.this.addElementToEnd(element);
			return true;
		}

		@Override
		public boolean remove(Object object) {
			return this.removeFirstOccurrence(object);
		}

		@Override
		public boolean containsAll(@NotNull Collection<?> collection) {
			for (Object object : collection) {
				if (!this.contains(object)) return false;
			}
			return true;
		}

		@Override
		public boolean addAll(@NotNull Collection<? extends T> collection) {
			if (collection.isEmpty()) return false;
			if (collection == this) throw new IllegalArgumentException("Cannot add list to itself");
			LinkedArrayList.this.addListToEnd(LinkedArrayList.ofNodes(collection.stream().map(Node::new).toArray(Node[]::new)));
			return true;
		}

		@Override
		public boolean addAll(int index, @NotNull Collection<? extends T> collection) {
			if (collection.isEmpty()) return false;
			if (collection == this) throw new IllegalArgumentException("Cannot add list to itself");
			LinkedArrayList.this.insertListBefore(LinkedArrayList.this.getNode(index, false), LinkedArrayList.ofNodes(collection.stream().map(Node::new).toArray(Node[]::new)));
			return true;
		}

		@Override
		public boolean removeAll(@NotNull Collection<?> collection) {
			if (collection.isEmpty()) return false;
			if (collection == this) {
				LinkedArrayList.this.clear();
				return true;
			}
			return this.removeIf(collection::contains);
		}

		@Override
		public boolean removeIf(Predicate<? super T> filter) {
			boolean removed = false;
			for (Node<T> node = LinkedArrayList.this.first; node != null;) {
				Node<T> next = node.next;
				if (filter.test(node.element)) {
					LinkedArrayList.this.removeNode(node);
					removed = true;
				}
				node = next;
			}
			return removed;
		}

		@Override
		public boolean retainAll(@NotNull Collection<?> collection) {
			if (collection == this || this.isEmpty()) return false;
			if (collection.isEmpty()) {
				this.clear();
				return true;
			}
			return this.removeIf((T element) -> !collection.contains(element));
		}

		@Override
		public void replaceAll(UnaryOperator<T> operator) {
			for (Node<T> node = LinkedArrayList.this.first; node != null; node = node.next) {
				node.element = operator.apply(node.element);
			}
		}

		@Override
		public void sort(Comparator<? super T> comparator) {
			LinkedArrayList.this.sortElements(comparator);
		}

		@Override
		public void clear() {
			LinkedArrayList.this.clear();
		}

		@Override
		public T get(int index) {
			return LinkedArrayList.this.getElement(index, true);
		}

		@Override
		public T set(int index, T element) {
			Node<T> node = LinkedArrayList.this.getNode(index, true);
			T oldValue = node.element;
			node.element = element;
			return oldValue;
		}

		@Override
		public void add(int index, T element) {
			LinkedArrayList.this.insertElementBefore(LinkedArrayList.this.getNode(index, false), element);
		}

		@Override
		public T remove(int index) {
			Node<T> node = LinkedArrayList.this.getNode(index, false);
			LinkedArrayList.this.removeNode(node);
			return node.element;
		}

		@Override
		public int indexOf(Object object) {
			return LinkedArrayList.this.indexOfElement(object, 0);
		}

		@Override
		public int lastIndexOf(Object object) {
			return LinkedArrayList.this.lastIndexOfElement(object, this.size() - 1);
		}

		@NotNull
		@Override
		public ListIterator<T> listIterator() {
			ElementListIterator iterator = new ElementListIterator();
			iterator.next = LinkedArrayList.this.first;
			return iterator;
		}

		@NotNull
		@Override
		public ListIterator<T> listIterator(int index) {
			Node<T> node = LinkedArrayList.this.getNode(index, false);
			ElementListIterator iterator = new ElementListIterator();
			iterator.next = node;
			iterator.prev = node.prev;
			return iterator;
		}

		@Override
		public Spliterator<T> spliterator() {
			int size = LinkedArrayList.this.size;
			if (size == 0) return Spliterators.emptySpliterator();
			Node<T>[] arrayCache = LinkedArrayList.this.arrayCache;
			if (arrayCache != null) return Spliterators.spliterator(arrayCache, Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
			return new ElementSpliterator<>(LinkedArrayList.this.getFirstNode(), size);
		}

		@Override
		public List<T> subList(int fromIndex, int toIndex) {
			return super.subList(fromIndex, toIndex);
		}

		@Override
		public void removeRange(int fromIndex, int toIndex) {
			if (toIndex < fromIndex) throw new IndexOutOfBoundsException(toIndex + " < " + fromIndex);
			if (toIndex == fromIndex) return;
			LinkedArrayList.this.removeNodeRange(LinkedArrayList.this.getNode(fromIndex, false), LinkedArrayList.this.getNode(toIndex - 1, false));
		}

		@Override
		public ElementList clone() {
			return LinkedArrayList.this.clone().elements();
		}
	}

	public static class BaseIterator<T> {

		public @Nullable Node<T> prev, next, current;

		public boolean baseHasNext() {
			return this.next != null;
		}

		public Node<T> nextNode() throws NoSuchElementException {
			Node<T> node = this.next;
			if (node == null) throw new NoSuchElementException();
			this.prev = node;
			this.next = node.next;
			this.current = node;
			return node;
		}

		public T nextElement() throws NoSuchElementException {
			return this.nextNode().element;
		}

		public boolean baseHasPrev() {
			return this.prev != null;
		}

		public Node<T> prevNode() throws NoSuchElementException {
			Node<T> node = this.prev;
			if (node == null) throw new NoSuchElementException();
			this.prev = node.prev;
			this.next = node;
			this.current = node;
			return node;
		}

		public T prevElement() throws NoSuchElementException {
			return this.prevNode().element;
		}

		public void forNextNodes(Consumer<? super Node<T>> action) {
			Node<T> node = this.next;
			if (node == null) return;
			while (true) {
				action.accept(node);
				Node<T> next = node.next;
				if (next != null) node = next;
				else break;
			}
			this.prev = node;
			this.current = node;
			this.next = null;
		}

		public void forNextElements(Consumer<? super T> action) {
			Node<T> node = this.next;
			if (node == null) return;
			while (true) {
				action.accept(node.element);
				Node<T> next = node.next;
				if (next != null) node = next;
				else break;
			}
			this.prev = node;
			this.current = node;
			this.next = null;
		}

		public void forPrevNodes(Consumer<? super Node<T>> action) {
			Node<T> node = this.prev;
			if (node == null) return;
			while (true) {
				action.accept(node);
				Node<T> prev = node.prev;
				if (prev != null) node = prev;
				else break;
			}
			this.prev = null;
			this.current = node;
			this.next = node;
		}

		public void forPrevElements(Consumer<? super T> action) {
			Node<T> node = this.prev;
			if (node == null) return;
			while (true) {
				action.accept(node.element);
				Node<T> prev = node.prev;
				if (prev != null) node = prev;
				else break;
			}
			this.prev = null;
			this.current = node;
			this.next = node;
		}
	}

	public static class NodeIterator<T> extends BaseIterator<T> implements Iterator<Node<T>> {

		@Override
		public boolean hasNext() {
			return this.baseHasNext();
		}

		@Override
		public Node<T> next() {
			return this.nextNode();
		}

		@Override
		public void forEachRemaining(Consumer<? super Node<T>> action) {
			this.forNextNodes(action);
		}
	}

	public static class ElementIterator<T> extends BaseIterator<T> implements Iterator<T> {

		@Override
		public boolean hasNext() {
			return this.baseHasNext();
		}

		@Override
		public T next() {
			return this.nextElement();
		}

		@Override
		public void forEachRemaining(Consumer<? super T> action) {
			this.forNextElements(action);
		}
	}

	public static class DescendingNodeIterator<T> extends BaseIterator<T> implements Iterator<Node<T>> {

		@Override
		public boolean hasNext() {
			return this.baseHasPrev();
		}

		@Override
		public Node<T> next() {
			return this.prevNode();
		}

		@Override
		public void forEachRemaining(Consumer<? super Node<T>> action) {
			this.forPrevNodes(action);
		}
	}

	public static class DescendingElementIterator<T> extends BaseIterator<T> implements Iterator<T> {

		@Override
		public boolean hasNext() {
			return this.baseHasPrev();
		}

		@Override
		public T next() {
			return this.prevElement();
		}

		@Override
		public void forEachRemaining(Consumer<? super T> action) {
			this.forPrevElements(action);
		}
	}

	public class BaseListIterator extends BaseIterator<T> {

		public int nextIndex() {
			Node<T> node = this.next;
			return node != null ? LinkedArrayList.this.indexOfNode(node, true) : LinkedArrayList.this.size();
		}

		public int previousIndex() {
			Node<T> node = this.prev;
			return node != null ? LinkedArrayList.this.indexOfNode(node, true) : -1;
		}

		public void remove() {
			Node<T> node = this.current;
			if (node == null) throw new NoSuchElementException();
			Node<T> prev = node.prev, next = node.next;
			LinkedArrayList.this.removeNode(node);
			this.prev = prev;
			this.next = next;
			this.current = null;
		}

		public void setNode(Node<T> replacement) {
			Node<T> node = this.current;
			if (node == null) throw new IllegalStateException();
			LinkedArrayList.this.replaceNode(node, replacement);
			this.current = replacement;
		}

		public void setElement(T replacement) {
			Node<T> node = this.current;
			if (node == null) throw new IllegalStateException();
			node.element = replacement;
		}

		public void addNode(Node<T> toAdd) {
			if (this.next != null) LinkedArrayList.this.insertNodeBefore(this.next, toAdd);
			else LinkedArrayList.this.addNodeToEnd(toAdd);
			this.prev = toAdd;
			this.current = null;
		}

		public void addElement(T toAdd) {
			this.addNode(new Node<>(toAdd));
		}
	}

	public class NodeListIterator extends BaseListIterator implements ListIterator<Node<T>> {

		@Override
		public boolean hasNext() {
			return this.baseHasNext();
		}

		@Override
		public boolean hasPrevious() {
			return this.baseHasPrev();
		}

		@Override
		public Node<T> next() {
			return this.nextNode();
		}

		@Override
		public Node<T> previous() {
			return this.prevNode();
		}

		@Override
		public void set(Node<T> node) {
			this.setNode(node);
		}

		@Override
		public void add(Node<T> node) {
			this.addNode(node);
		}

		@Override
		public void forEachRemaining(Consumer<? super Node<T>> action) {
			this.forNextNodes(action);
		}
	}

	public class ElementListIterator extends BaseListIterator implements ListIterator<T> {

		@Override
		public boolean hasNext() {
			return this.baseHasNext();
		}

		@Override
		public boolean hasPrevious() {
			return this.baseHasPrev();
		}

		@Override
		public T next() {
			return this.nextElement();
		}

		@Override
		public T previous() {
			return this.prevElement();
		}

		@Override
		public void set(T replacement) {
			this.setElement(replacement);
		}

		@Override
		public void add(T toAdd) {
			this.addElement(toAdd);
		}

		@Override
		public void forEachRemaining(Consumer<? super T> action) {
			this.forNextElements(action);
		}
	}

	public static class BaseSpliterator<T> {

		public Node<T> head;
		public int remaining;

		public BaseSpliterator(Node<T> head, int remaining) {
			this.head = head;
			this.remaining = remaining;
		}

		public Node<T> nextNode() {
			int remaining = this.remaining;
			if (remaining == 0) return null;
			this.remaining = remaining - 1;
			Node<T> node = this.head;
			this.head = node.next;
			return node;
		}

		public boolean advanceNode(Consumer<? super Node<T>> action) {
			Node<T> node = this.nextNode();
			if (node == null) return false;
			action.accept(node);
			return true;
		}

		public boolean advanceElement(Consumer<? super T> action) {
			Node<T> node = this.nextNode();
			if (node == null) return false;
			action.accept(node.element);
			return true;
		}

		public void forAllNodes(Consumer<? super Node<T>> action) {
			Node<T> node = this.head;
			for (int remaining = this.remaining; remaining-- != 0;) {
				action.accept(node);
				node = node.getNext();
			}
			this.head = null;
			this.remaining = 0;
		}

		public void forAllElements(Consumer<? super T> action) {
			Node<T> node = this.head;
			for (int remaining = this.remaining; remaining-- != 0;) {
				action.accept(node.element);
				node = node.getNext();
			}
			this.head = null;
			this.remaining = 0;
		}

		public Node<T> skip(int count) {
			Node<T> head = this.head;
			Node<T> tail = head;
			for (int i = count; i-- != 0;) {
				tail = tail.getNext();
			}
			this.remaining -= count;
			this.head = tail;
			return head;
		}

		public long estimateSize() {
			return this.remaining;
		}

		public long getExactSizeIfKnown() {
			return this.remaining;
		}

		public int characteristics() {
			return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
		}
	}

	public static class NodeSpliterator<T> extends BaseSpliterator<T> implements Spliterator<Node<T>> {

		public NodeSpliterator(Node<T> head, int remaining) {
			super(head, remaining);
		}

		@Override
		public boolean tryAdvance(Consumer<? super Node<T>> action) {
			return this.advanceNode(action);
		}

		@Override
		public void forEachRemaining(Consumer<? super Node<T>> action) {
			this.forAllNodes(action);
		}

		@Override
		public Spliterator<Node<T>> trySplit() {
			int toSkip = this.remaining >> 1;
			return toSkip != 0 ? new NodeSpliterator<>(this.skip(toSkip), toSkip) : null;
		}
	}

	public static class ElementSpliterator<T> extends BaseSpliterator<T> implements Spliterator<T> {

		public ElementSpliterator(Node<T> head, int remaining) {
			super(head, remaining);
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			return this.advanceElement(action);
		}

		@Override
		public void forEachRemaining(Consumer<? super T> action) {
			this.forAllElements(action);
		}

		@Override
		public Spliterator<T> trySplit() {
			int toSkip = this.remaining >> 1;
			return toSkip != 0 ? new ElementSpliterator<>(this.skip(toSkip), toSkip) : null;
		}
	}

	public static class Node<T> implements Cloneable {

		@SuppressWarnings("rawtypes")
		public static final Node[] EMPTY_ARRAY = new Node[0];

		/** the index to use for nodes which are not currently in a list */
		public static final int NOT_IN_LIST = -1;

		/** the current element that this node holds. */
		public T element;

		/** if this node is currently in a list, prev and next represent the nodes before and after it in that list. */
		public @Nullable Node<T> prev, next;

		/**
		an internal tracker for LinkedArrayList's caching system.
		if this node is currently in a list, then this value represents the node's index in that list.
		otherwise, the value is set to NOT_IN_LIST.
		note: any addition/removal of nodes from a list will NOT shift the remaining indexes.
		as such, this value should NOT be trusted to be correct!
		instead, you should use LinkedArrayList.indexOfNode(node).
		*/
		public int index = NOT_IN_LIST;

		public Node(T element) {
			this.element = element;
		}

		/**
		returns the previous node if it exists
		if it does not exist, throws a NoSuchElementException
		*/
		@NotNull
		public Node<T> getPrev() {
			Node<T> prev;
			if ((prev = this.prev) != null) return prev;
			else throw new NoSuchElementException("No node before " + this);
		}

		/**
		returns the next node if it exists
		if it does not exist, throws a NoSuchElementException
		*/
		@NotNull
		public Node<T> getNext() {
			Node<T> next;
			if ((next = this.next) != null) return next;
			else throw new NoSuchElementException("No node after " + this);
		}

		/**
		returns the element, always.
		for use in method references.
		*/
		public T getElement() {
			return this.element;
		}

		@Override
		public Node<T> clone() {
			try {
				@SuppressWarnings("unchecked")
				Node<T> clone = (Node<T>)(super.clone());
				clone.prev = null;
				clone.next = null;
				clone.index = NOT_IN_LIST;
				return clone;
			}
			catch (CloneNotSupportedException e) {
				throw new InternalError(e);
			}
		}

		//////////////////////////////// Utility methods for LinkedArrayList ////////////////////////////////

		public boolean isInList() {
			return this.index != NOT_IN_LIST;
		}

		public boolean isNotInList() {
			return this.index == NOT_IN_LIST;
		}

		public void ensureInList() {
			if (this.isNotInList()) {
				throw new IllegalStateException(this + " is not in any list");
			}
		}

		public void ensureNotInList() {
			if (this.isInList()) {
				throw new IllegalStateException(this + " is already in another list");
			}
			if (this.prev != null || this.next != null) {
				throw new IllegalStateException(this + " has dangling links: " + this.prev + " and " + this.next);
			}
		}

		/**
		called when this node is added to a list.
		returns itself for convenience.
		*/
		public Node<T> initListIndex(int index) {
			this.ensureNotInList();
			this.index = index;
			return this;
		}

		/** called when this node is removed from a list. */
		public void removedFromList() {
			this.ensureInList();
			this.index = NOT_IN_LIST;
			this.next = null;
			this.prev = null;
		}

		public Node<T> removeAndGetNext() {
			Node<T> next = this.getNext();
			this.removedFromList();
			return next;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.element);
		}

		public boolean equals(Node<?> that) {
			return Objects.equals(this.element, that.element);
		}

		@Override
		public boolean equals(Object that) {
			return that instanceof Node<?> && this.equals((Node<?>)(that));
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder(64);
			if (this.prev != null) this.prev.toString0(s).append(" <- ");
			this.toString0(s);
			if (this.next != null) this.next.toString0(s.append(" -> "));
			return s.toString();
		}

		public StringBuilder toString0(StringBuilder s) {
			return s.append(this.getClass().getSimpleName()).append(": { ").append(this.element).append(" }");
		}
	}
}