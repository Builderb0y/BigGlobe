package builderb0y.bigglobe.scripting.wrappers;

import java.util.*;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConstantMap<K, V> extends AbstractMap<K, V> {

	public static final Object NULL = new Object() {

		@Override
		public String toString() {
			return "null signaler";
		}
	};

	public static <K> @NotNull Object wrap(@Nullable K object) {
		return object == null ? NULL : object;
	}

	@SuppressWarnings("unchecked")
	public static <K> @Nullable K unwrap(@NotNull Object object) {
		return object == NULL ? null : (K)(object);
	}

	public final K[] keys;
	public final V[] values;
	public final int[] order;
	public transient EntrySet entrySet;

	@SuppressWarnings("unchecked")
	public ConstantMap() {
		this.keys   = (K[])(ObjectArrays.EMPTY_ARRAY);
		this.values = (V[])(ObjectArrays.EMPTY_ARRAY);
		this.order  = IntArrays.EMPTY_ARRAY;
	}

	@SuppressWarnings("unchecked")
	public ConstantMap(Object... keysAndValues) {
		int keyValueLength = keysAndValues.length;
		if ((keyValueLength & 1) != 0) {
			throw new IllegalArgumentException("Odd number of arguments");
		}
		int capacity = Integer.highestOneBit(keyValueLength);
		int mask = capacity - 1;
		int size = keyValueLength >>> 1;
		Object[] keys   = new Object[capacity];
		Object[] values = new Object[capacity];
		int[] order = new int[size];
		for (int orderIndex = 0, keyValueIndex = 0; keyValueIndex < keyValueLength;) {
			Object key   = keysAndValues[keyValueIndex++];
			Object value = keysAndValues[keyValueIndex++];
			if (key == null) key = NULL;
			int position = key.hashCode();
			position ^= position >>> 16;
			position &= mask;
			while (keys[position] != null) {
				if (keys[position].equals(key)) {
					throw new IllegalArgumentException("Duplicate key: " + key);
				}
				position = (position + 1) & mask;
			}
			keys[position] = key;
			values[position] = value;
			order[orderIndex++] = position;
		}
		this.keys   = (K[])(keys);
		this.values = (V[])(values);
		this.order  = order;
	}

	@Override
	public int size() {
		return this.order.length;
	}

	@Override
	public boolean containsKey(Object key) {
		return this.getPosition(key) >= 0;
	}

	@Override
	public boolean containsValue(Object value) {
		V[] values = this.values;
		if (value != null) {
			for (int position : this.order) {
				if (value.equals(values[position])) return true;
			}
		}
		else {
			for (int position : this.order) {
				if (values[position] == null) return true;
			}
		}
		return false;
	}

	public int getPosition(Object key) {
		if (key == null) key = NULL;
		int position = key.hashCode();
		position ^= position >>> 16;
		K[] keys = this.keys;
		int mask = keys.length - 1;
		position &= mask;
		while (keys[position] != null) {
			if (keys[position].equals(key)) return position;
			position = (position + 1) & mask;
		}
		return -1;
	}

	@Override
	public @Nullable V get(Object key) {
		int position = this.getPosition(key);
		return position >= 0 ? this.values[position] : null;
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		int position = this.getPosition(key);
		return position >= 0 ? this.values[position] : defaultValue;
	}

	@Override
	public @NotNull Set<Map.Entry<K, V>> entrySet() {
		return this.entrySet == null ? this.entrySet = this.new EntrySet() : this.entrySet;
	}

	public class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return ConstantMap.this.new EntryIterator();
		}

		@Override
		public int size() {
			return ConstantMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof Map.Entry<?, ?> entry) {
				int position = ConstantMap.this.getPosition(entry.getKey());
				return position >= 0 && Objects.equals(entry.getValue(), ConstantMap.this.values[position]);
			}
			return false;
		}
	}

	public class EntryIterator implements Iterator<Map.Entry<K, V>> {

		public int index;

		@Override
		public boolean hasNext() {
			return this.index < ConstantMap.this.order.length;
		}

		@Override
		public Map.Entry<K, V> next() {
			if (this.index >= ConstantMap.this.order.length) throw new NoSuchElementException();
			int position = ConstantMap.this.order[this.index++];
			return new Entry<>(unwrap(ConstantMap.this.keys[position]), ConstantMap.this.values[position]);
		}
	}

	public static record Entry<K, V>(K getKey, V getValue) implements Map.Entry<K, V> {

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean equals(Object obj) {
			return (
				obj instanceof Map.Entry<?, ?> that &&
				Objects.equals(this.getKey(), that.getKey()) &&
				Objects.equals(this.getValue(), that.getValue())
			);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.getKey()) ^ Objects.hashCode(this.getValue());
		}

		@Override
		public String toString() {
			return this.getKey() + " -> " + this.getValue();
		}
	}
}