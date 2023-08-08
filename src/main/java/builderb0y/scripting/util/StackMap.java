package builderb0y.scripting.util;

import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/**
a Map implementation which supports the concept of stack frames.
frames can be pushed and popped, and these two operations come in nested pairs.
every call to {@link #pop()} will remove all entries from the
map which were added after the last call to {@link #push()}.
example usage: {@code
	map.push();
	map.put("a", 1)
	map.push();
	map.put("b", 2)
	//map now contains { "a": 1, "b": 2 }
	map.pop();
	//map now contains { "a": 1 }
	map.pop();
	//map is now empty.
}
*/
public class StackMap<K, V> extends Object2ObjectLinkedOpenHashMap<K, V> {

	public IntArrayList sizes;

	public StackMap() {
		this.sizes = new IntArrayList(16);
	}

	public StackMap(int expected) {
		super(expected);
		this.sizes = new IntArrayList(16);
	}

	public StackMap(StackMap<? extends K, ? extends V> from) {
		super(from);
		this.sizes = new IntArrayList(from.sizes);
	}

	public void push() {
		this.sizes.add(this.size());
	}

	public void pop() {
		int size = this.sizes.removeInt(this.sizes.size() - 1);
		while (this.size() > size) {
			this.removeLast();
		}
	}

	/** default implementation is bugged and does not fix pointers correctly. */
	@Override
	public V removeLast() {
		if (this.size == 0) throw new NoSuchElementException();
		int pos = this.last;
		V value = this.value[pos];
		this.value[pos] = null;
		this.size--;
		this.fixPointers(pos);
		this.shiftKeys(pos);
		if (this.n > this.minN && this.size < this.maxFill >> 2 && this.n > DEFAULT_INITIAL_SIZE) {
			this.rehash(this.n >> 1);
		}
		return value;
	}

	/**
	returns true if, and only if, new entries have been
	added to this map since the last call to {@link #push()}.
	otherwise, returns false.
	*/
	public boolean hasNewElements() {
		return this.size() > this.sizes.getInt(this.sizes.size() - 1);
	}

	@Override
	public void clear() {
		super.clear();
		this.sizes.clear();
	}
}