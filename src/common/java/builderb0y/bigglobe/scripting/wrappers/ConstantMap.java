package builderb0y.bigglobe.scripting.wrappers;

import java.util.*;
import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import net.minecraft.util.Mth;

public class ConstantMap<K, V> extends AbstractMap<K, V> implements SequencedMap<K, V> {

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
		this.keys = (K[])(ObjectArrays.EMPTY_ARRAY);
		this.values = (V[])(ObjectArrays.EMPTY_ARRAY);
		this.order = IntArrays.EMPTY_ARRAY;
	}

	@SuppressWarnings("unchecked")
	public ConstantMap(Contents<K, V> contents) {
		Objects.requireNonNull(contents, "contents");
		int size = contents.size();
		if (size == 0) {
			this.keys = (K[])(ObjectArrays.EMPTY_ARRAY);
			this.values = (V[])(ObjectArrays.EMPTY_ARRAY);
			this.order = IntArrays.EMPTY_ARRAY;
			return;
		}
		int capacity = Mth.smallestEncompassingPowerOfTwo(size + (size >> 2) + 1);
		int mask = capacity - 1;
		Object[] keys = new Object[capacity];
		Object[] values = new Object[capacity];
		int[] order = new int[size];
		for (int orderIndex = 0; orderIndex < size; orderIndex++) {
			Object key = wrap(contents.getKey(orderIndex));
			Object value = contents.getValue(orderIndex);
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
			order[orderIndex] = position;
		}
		this.keys = (K[])(keys);
		this.values = (V[])(values);
		this.order = order;
	}

	@SuppressWarnings("unchecked")
	public ConstantMap(Object... keysAndValues) {
		if ((keysAndValues.length & 1) != 0) {
			throw new IllegalArgumentException("key/value array has odd length.");
		}
		this(new Contents<>() {

			@Override
			public int size() {
				return keysAndValues.length >>> 1;
			}

			@Override
			public K getKey(int index) {
				return (K)(keysAndValues[index << 1]);
			}

			@Override
			public V getValue(int index) {
				return (V)(keysAndValues[(index << 1) | 1]);
			}
		});
	}

	@SafeVarargs
	public <T> ConstantMap(
		Function<? super T, ? extends K> keyExtractor,
		Function<? super T, ? extends V> valueExtractor,
		T... elements
	) {
		Objects.requireNonNull(keyExtractor, "keyExtractor");
		Objects.requireNonNull(valueExtractor, "valueExtractor");
		Objects.requireNonNull(elements, "elements");
		this(new Contents<>() {

			@Override
			public int size() {
				return elements.length;
			}

			@Override
			public K getKey(int index) {
				return keyExtractor.apply(elements[index]);
			}

			@Override
			public V getValue(int index) {
				return valueExtractor.apply(elements[index]);
			}
		});
	}

	public static interface Contents<K, V> {

		public abstract int size();

		public abstract K getKey(int index);

		public abstract V getValue(int index);
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
	public SequencedMap<K, V> reversed() {
		return this.new ReversedView();
	}

	@Override
	public @NotNull SequencedSet<Map.Entry<K, V>> entrySet() {
		return this.entrySet == null ? this.entrySet = this.new EntrySet() : this.entrySet;
	}

	public class EntrySet extends AbstractSet<Map.Entry<K, V>> implements SequencedSet<Map.Entry<K, V>> {

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

		@Override
		public @NonNull SequencedSet<Map.Entry<K, V>> reversed() {
			return this.new ReversedEntrySet();
		}

		public class ReversedEntrySet extends AbstractSet<Map.Entry<K, V>> implements SequencedSet<Map.Entry<K, V>> {

			@Override
			public boolean contains(Object o) {
				return EntrySet.this.contains(o);
			}

			@Override
			public Iterator<Map.Entry<K, V>> iterator() {
				return ConstantMap.this.new ReversedEntryIterator();
			}

			@Override
			public int size() {
				return ConstantMap.this.size();
			}

			@Override
			public @NonNull SequencedSet<Map.Entry<K, V>> reversed() {
				return EntrySet.this;
			}
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
			return new ConstantMap.Entry<>(unwrap(ConstantMap.this.keys[position]), ConstantMap.this.values[position]);
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

	public class ReversedView extends AbstractMap<K, V> implements SequencedMap<K, V> {

		@Override
		public int size() {
			return ConstantMap.this.size();
		}

		@Override
		public boolean containsKey(Object key) {
			return ConstantMap.this.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return ConstantMap.this.containsValue(value);
		}

		@Override
		public V get(Object key) {
			return ConstantMap.this.get(key);
		}

		@Override
		public V getOrDefault(Object key, V defaultValue) {
			return ConstantMap.this.getOrDefault(key, defaultValue);
		}

		@Override
		public @NonNull Set<Entry<K, V>> entrySet() {
			return ConstantMap.this.entrySet().reversed();
		}

		@Override
		public SequencedMap<K, V> reversed() {
			return ConstantMap.this;
		}
	}

	public class ReversedEntryIterator implements Iterator<Map.Entry<K, V>> {

		public int index = ConstantMap.this.size() - 1;

		@Override
		public boolean hasNext() {
			return this.index >= 0;
		}

		@Override
		public Map.Entry<K, V> next() {
			if (this.index < 0) throw new NoSuchElementException();
			int position = ConstantMap.this.order[this.index--];
			return new ConstantMap.Entry<>(unwrap(ConstantMap.this.keys[position]), ConstantMap.this.values[position]);
		}
	}
}