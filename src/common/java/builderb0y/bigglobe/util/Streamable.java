package builderb0y.bigglobe.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Streamable<T> {

	public abstract Stream<T> stream();

	public static <T> Stream<T> stream(Iterable<T> iterable) {
		return iterable instanceof Collection<T> collection ? collection.stream() : StreamSupport.stream(iterable.spliterator(), false);
	}

	public static <T> Streamable<T> of(Iterable<T> iterable) {
		return iterable instanceof Collection<T> collection ? collection::stream : () -> StreamSupport.stream(iterable.spliterator(), false);
	}

	public static <T> Streamable<T> singleton(T element) {
		return () -> Stream.of(element);
	}

	public static <T> Streamable<T> empty() {
		return Stream::empty;
	}

	public static class StreamableArrayList<T> extends ArrayList<T> implements Streamable<T> {

		public StreamableArrayList() {}

		public StreamableArrayList(int initialCapacity) {
			super(initialCapacity);
		}

		public StreamableArrayList(@NotNull Collection<? extends T> c) {
			super(c);
		}

		@Override
		public @NotNull Stream<T> stream() {
			return super.stream();
		}
	}

	public static class StreamableHashSet<T> extends HashSet<T> implements Streamable<T> {

		public StreamableHashSet() {}

		public StreamableHashSet(@NotNull Collection<? extends T> c) {
			super(c);
		}

		public StreamableHashSet(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
		}

		public StreamableHashSet(int initialCapacity) {
			super(initialCapacity);
		}

		@Override
		public @NotNull Stream<T> stream() {
			return super.stream();
		}
	}
}