package builderb0y.bigglobe.util;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.autocodec.annotations.Wrapper;

public class LazyRegistryObjectCollection<E, C extends Collection<E>> implements Collection<E> {

	public final @SingletonArray List<TagOrObject<E>> collected;
	public transient C collection;
	public transient Supplier<C> collectionFactory;

	public LazyRegistryObjectCollection(List<TagOrObject<E>> collected, Supplier<C> collectionFactory) {
		this.collected = collected;
		this.collectionFactory = collectionFactory;
	}

	public C delegate() {
		if (this.collection == null) {
			this.collection = this.collectionFactory.get();
			this.collectionFactory = null;
			for (TagOrObject<E> entries : this.collected) {
				for (RegistryEntry<E> entry : entries) {
					this.collection.add(entry.value());
				}
			}
		}
		return this.collection;
	}

	@Override
	public int size() {
		return this.delegate().size();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate().isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.delegate().contains(o);
	}

	@NotNull
	@Override
	public Iterator<E> iterator() {
		return new UnmodifiableIterator<>(this.delegate().iterator());
	}

	@NotNull
	@Override
	public Object[] toArray() {
		return this.delegate().toArray();
	}

	@NotNull
	@Override
	public <T> T[] toArray(@NotNull T[] a) {
		return this.delegate().toArray(a);
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return this.delegate().containsAll(c);
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(IntFunction<T[]> generator) {
		return this.delegate().toArray(generator);
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Spliterator<E> spliterator() {
		return this.delegate().spliterator();
	}

	@Override
	public Stream<E> stream() {
		return this.delegate().stream();
	}

	@Override
	public Stream<E> parallelStream() {
		return this.delegate().parallelStream();
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		this.delegate().forEach(action);
	}

	public static class UnmodifiableIterator<E> implements Iterator<E> {

		public final Iterator<E> delegate;

		public UnmodifiableIterator(Iterator<E> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean hasNext() {
			return this.delegate.hasNext();
		}

		@Override
		public E next() {
			return this.delegate.next();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			this.delegate.forEachRemaining(action);
		}
	}

	public static class LazyRegistryObjectList<E, L extends List<E>> extends LazyRegistryObjectCollection<E, L> implements List<E> {

		public LazyRegistryObjectList(@SingletonArray List<TagOrObject<E>> collected, Supplier<L> collectionFactory) {
			super(collected, collectionFactory);
		}

		@Override
		public boolean addAll(int index, @NotNull Collection<? extends E> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public E get(int index) {
			return this.delegate().get(index);
		}

		@Override
		public E set(int index, E element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(int index, E element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public E remove(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int indexOf(Object o) {
			return this.delegate().indexOf(o);
		}

		@Override
		public int lastIndexOf(Object o) {
			return this.delegate().lastIndexOf(o);
		}

		@NotNull
		@Override
		public ListIterator<E> listIterator() {
			return new UnmodifiableListIterator<>(this.delegate().listIterator());
		}

		@NotNull
		@Override
		public ListIterator<E> listIterator(int index) {
			return new UnmodifiableListIterator<>(this.delegate().listIterator(index));
		}

		@NotNull
		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			return Collections.unmodifiableList(this.delegate().subList(fromIndex, toIndex));
		}

		@Override
		public void replaceAll(UnaryOperator<E> operator) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void sort(Comparator<? super E> c) {
			throw new UnsupportedOperationException();
		}

		public static class UnmodifiableListIterator<E> implements ListIterator<E> {

			public final ListIterator<E> delegate;

			public UnmodifiableListIterator(ListIterator<E> delegate) {
				this.delegate = delegate;
			}

			@Override
			public boolean hasNext() {
				return this.delegate.hasNext();
			}

			@Override
			public E next() {
				return this.delegate.next();
			}

			@Override
			public void forEachRemaining(Consumer<? super E> action) {
				this.delegate.forEachRemaining(action);
			}

			@Override
			public boolean hasPrevious() {
				return this.delegate.hasPrevious();
			}

			@Override
			public E previous() {
				return this.delegate.previous();
			}

			@Override
			public int nextIndex() {
				return this.delegate.nextIndex();
			}

			@Override
			public int previousIndex() {
				return this.delegate.previousIndex();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void set(E e) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void add(E e) {
				throw new UnsupportedOperationException();
			}
		}

		@Wrapper
		public static class Impl<E> extends LazyRegistryObjectList<E, ArrayList<E>> {

			public Impl(@SingletonArray List<TagOrObject<E>> collected) {
				super(collected, ArrayList::new);
			}
		}
	}

	public static class LazyRegistryObjectSet<E, S extends Set<E>> extends LazyRegistryObjectCollection<E, S> implements Set<E> {

		public LazyRegistryObjectSet(@SingletonArray List<TagOrObject<E>> collected, Supplier<S> collectionFactory) {
			super(collected, collectionFactory);
		}

		@Wrapper
		public static class Impl<E> extends LazyRegistryObjectSet<E, HashSet<E>> {

			public Impl(@SingletonArray List<TagOrObject<E>> collected) {
				super(collected, HashSet::new);
			}
		}
	}
}