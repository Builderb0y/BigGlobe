package builderb0y.bigglobe.randomLists;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import builderb0y.bigglobe.randomLists.IRandomList.RandomAccessKnownTotalWeightRandomList;

@Unmodifiable
public class EmptyRandomList<E> implements RandomAccessKnownTotalWeightRandomList<E> {

	public static final EmptyRandomList<?> INSTANCE = new EmptyRandomList<>();

	@SuppressWarnings("unchecked")
	public static <E> EmptyRandomList<E> instance() {
		return (EmptyRandomList<E>)(INSTANCE);
	}

	//////////////////////////////// get ////////////////////////////////

	@Override
	public E get(int index) {
		throw outOfBounds(index);
	}

	@Override
	public double getWeight(int index) {
		throw outOfBounds(index);
	}

	@Override
	public double getTotalWeight() {
		return 0.0D;
	}

	@Override
	public E getRandomElement(long seed) {
		throw new NoSuchElementException();
	}

	@Override
	public E getRandomElement(RandomGenerator random) {
		throw new NoSuchElementException();
	}

	//////////////////////////////// set ////////////////////////////////

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double setWeight(int index, double weight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E set(int index, E element, double weight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		Objects.requireNonNull(operator, "operator");
		//no-op
	}

	@Override
	public void replaceAllWeights(ToDoubleFunction<? super E> operator) {
		Objects.requireNonNull(operator, "operator");
		//no-op
	}

	//////////////////////////////// contains ////////////////////////////////

	@Override
	public boolean contains(Object o) {
		return false;
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return c.isEmpty();
	}

	@Override
	public int indexOf(Object o) {
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		return -1;
	}

	//////////////////////////////// size/empty ////////////////////////////////

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean isWeightless() {
		return true;
	}

	@Override
	public boolean isEmptyOrWeightless() {
		return true;
	}

	//////////////////////////////// add ////////////////////////////////

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(E element, double weight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, E element, double weight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, @NotNull Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	//////////////////////////////// remove ////////////////////////////////

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		return false;
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		return false;
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		Objects.requireNonNull(filter, "filter");
		return false;
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		return false;
	}

	@Override
	public void clear() {
		//no-op
	}

	//////////////////////////////// iterators ////////////////////////////////

	@Override
	public WeightedIterator<E> iterator() {
		return Itr.instance();
	}

	@Override
	public WeightedListIterator<E> listIterator() {
		return ListItr.instance();
	}

	@Override
	public WeightedListIterator<E> listIterator(int index) {
		checkIndexForAdd(index);
		return ListItr.instance();
	}

	@Override
	public Spliterator<E> spliterator() {
		return Spliterators.emptySpliterator();
	}

	@Override
	public Stream<E> stream() {
		return Stream.empty();
	}

	@Override
	public Stream<E> parallelStream() {
		return Stream.empty();
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		Objects.requireNonNull(action, "action");
		//no-op.
	}

	//////////////////////////////// view ////////////////////////////////

	@Override
	public Object[] toArray() {
		return ObjectArrays.EMPTY_ARRAY;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if (a.length != 0) a[0] = null;
		return a;
	}

	@Override
	public IRandomList<E> subList(int fromIndex, int toIndex) {
		if (fromIndex != 0 || toIndex != 0) {
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", size: 0");
		}
		return this;
	}

	@Override
	public IRandomList<E> optimize() {
		return this;
	}

	//////////////////////////////// other ////////////////////////////////

	@Override
	public void sort(Comparator<? super E> comparator) {
		Objects.requireNonNull(comparator, "comparator");
		//no-op
	}

	public static IndexOutOfBoundsException outOfBounds(int index) {
		return new IndexOutOfBoundsException("index: " + index + ", size: 0");
	}

	public static void checkIndexForAdd(int index) {
		if (index != 0) throw outOfBounds(index);
	}

	public static class Itr<E> implements WeightedIterator<E> {

		public static final Itr<?> INSTANCE = new Itr<>();

		@SuppressWarnings("unchecked")
		public static <E> Itr<E> instance() {
			return (Itr<E>)(INSTANCE);
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public E next() {
			throw new NoSuchElementException();
		}

		@Override
		public double getWeight() {
			throw new IllegalStateException();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			Objects.requireNonNull(action, "action");
			//no-op
		}
	}

	public static class ListItr<E> extends Itr<E> implements WeightedListIterator<E> {

		public static final ListItr<?> INSTANCE = new ListItr<>();

		@SuppressWarnings({ "unchecked", "MethodOverridesStaticMethodOfSuperclass" })
		public static <E> ListItr<E> instance() {
			return (ListItr<E>)(INSTANCE);
		}

		@Override
		public int nextIndex() {
			return 0;
		}

		@Override
		public boolean hasPrevious() {
			return false;
		}

		@Override
		public E previous() {
			throw new NoSuchElementException();
		}

		@Override
		public int previousIndex() {
			return -1;
		}

		@Override
		public void set(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setWeight(double weight) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}