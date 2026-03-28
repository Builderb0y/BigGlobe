package builderb0y.bigglobe.scripting.wrappers;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;

public class ConstantSet<K> extends AbstractSet<K> {

	public final K[] keys;
	public final int[] order;

	@SuppressWarnings("unchecked")
	public ConstantSet() {
		this.keys = (K[])(ObjectArrays.EMPTY_ARRAY);
		this.order = IntArrays.EMPTY_ARRAY;
	}

	@SuppressWarnings("unchecked")
	public ConstantSet(Object... args) {
		int size = args.length;
		int capacity = Integer.highestOneBit(size << 1);
		int mask = capacity - 1;
		Object[] keys = new Object[capacity];
		int[] order = new int[size];
		for (int index = 0; index < size; index++) {
			Object arg = ConstantMap.wrap(args[index]);
			int position = arg.hashCode();
			position ^= position >>> 16;
			position &= mask;
			while (keys[position] != null) {
				if (keys[position].equals(arg)) {
					throw new IllegalArgumentException("Duplicate element: " + arg);
				}
				position = (position + 1) & mask;
			}
			keys[position] = arg;
			order[index] = position;
		}
		this.keys = (K[])(keys);
		this.order = order;
	}

	@Override
	public int size() {
		return this.order.length;
	}

	@Override
	public boolean contains(Object element) {
		return this.getPosition(element) >= 0;
	}

	public int getPosition(Object element) {
		element = ConstantMap.wrap(element);
		int position = element.hashCode();
		position ^= position >>> 16;
		K[] keys = this.keys;
		int mask = keys.length - 1;
		position &= mask;
		while (keys[position] != null) {
			if (keys[position].equals(element)) return position;
			position = (position + 1) & mask;
		}
		return -1;
	}

	@Override
	public Iterator<K> iterator() {
		return this.new ConstantSetIterator();
	}

	public class ConstantSetIterator implements Iterator<K> {

		public int index;

		@Override
		public boolean hasNext() {
			return this.index < ConstantSet.this.order.length;
		}

		@Override
		public K next() {
			if (this.index >= ConstantSet.this.order.length) throw new NoSuchElementException();
			int position = ConstantSet.this.order[this.index++];
			return ConstantMap.unwrap(ConstantSet.this.keys[position]);
		}
	}
}