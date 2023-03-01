package builderb0y.bigglobe.randomLists;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import builderb0y.bigglobe.randomLists.IRandomList.RandomAccessKnownTotalWeightRandomList;

public class SingletonRandomList<E> implements RandomAccessKnownTotalWeightRandomList<E> {

	public E element;
	public double weight;

	public SingletonRandomList(E element, double weight) {
		this.element = element;
		this.weight = weight;
	}

	//////////////////////////////// get ////////////////////////////////

	@Override
	public E get(int index) {
		checkIndex(index);
		return this.element;
	}

	@Override
	public double getWeight(int index) {
		checkIndex(index);
		return this.weight;
	}

	@Override
	public double getTotalWeight() {
		return this.weight;
	}

	@Override
	public E getRandomElement(RandomGenerator random) {
		return this.weight > 0.0D ? this.element : null;
	}

	@Override
	public E getRandomElement(long seed) {
		return this.weight > 0.0D ? this.element : null;
	}

	//////////////////////////////// set ////////////////////////////////

	@Override
	public E set(int index, E element) {
		checkIndex(index);
		E oldElement = this.element;
		this.element = element;
		return oldElement;
	}

	@Override
	public E set(int index, E element, double weight) {
		checkIndex(index);
		E oldElement = this.element;
		this.element = element;
		this.weight = weight;
		return oldElement;
	}

	@Override
	public double setWeight(int index, double weight) {
		checkIndex(index);
		double oldWeight = this.weight;
		this.weight = weight;
		return oldWeight;
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		this.element = operator.apply(this.element);
	}

	@Override
	public void replaceAllWeights(ToDoubleFunction<? super E> operator) {
		this.weight = operator.applyAsDouble(this.element);
	}

	//////////////////////////////// contains ////////////////////////////////

	@Override
	public boolean contains(Object o) {
		return Objects.equals(o, this.element);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (!c.isEmpty()) {
			E element = this.element;
			if (element != null) {
				for (Object o : c) {
					if (!element.equals(o)) return false;
				}
			}
			else {
				for (Object o : c) {
					if (o != null) return false;
				}
			}
		}
		return true;
	}

	@Override
	public int indexOf(Object o) {
		return Objects.equals(o, this.element) ? 0 : -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		return Objects.equals(o, this.element) ? 0 : -1;
	}

	//////////////////////////////// size/empty ////////////////////////////////

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean isEmptyOrWeightless() {
		return this.isWeightless(); //isEmpty() is always false.
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
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	//////////////////////////////// remove ////////////////////////////////

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	//////////////////////////////// iterators ////////////////////////////////

	@Override
	public WeightedIterator<E> iterator() {
		return this.new Itr();
	}

	@Override
	public WeightedListIterator<E> listIterator() {
		return this.new ListItr();
	}

	@Override
	public WeightedListIterator<E> listIterator(int index) {
		checkIndexForAdd(index);
		ListItr iterator = this.new ListItr();
		if (index == 1) iterator.flags = ListItr.SKIPPED;
		return iterator;
	}

	@Override
	public Spliterator<E> spliterator() {
		return Collections.singletonList(this.element).spliterator();
	}

	@Override
	public Stream<E> stream() {
		return Stream.of(this.element);
	}

	@Override
	public Stream<E> parallelStream() {
		return Stream.of(this.element);
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		action.accept(this.element);
	}

	//////////////////////////////// view ////////////////////////////////

	@Override
	public Object[] toArray() {
		return new Object[] { this.element };
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		if (a.length == 0) a = (T[])(Array.newInstance(a.getClass().getComponentType(), 1));
		a[0] = (T)(this.element);
		if (a.length > 1) a[1] = null;
		return a;
	}

	@Override
	public IRandomList<E> subList(int fromIndex, int toIndex) {
		if (fromIndex < 0 || toIndex > 1 || toIndex < fromIndex) {
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", size: 1");
		}
		return fromIndex == toIndex ? EmptyRandomList.instance() : this;
	}

	@Override
	public IRandomList<E> optimizeSize() {
		return this;
	}

	//////////////////////////////// other ////////////////////////////////

	@Override
	public void sort(Comparator<? super E> c) {
		//no-op
	}

	public static void checkIndex(int index) {
		if (index != 0) throw new IndexOutOfBoundsException("index: " + index + ", size: 1");
	}

	public static void checkIndexForAdd(int index) {
		if (index != 0 && index != 1) throw new IndexOutOfBoundsException("index: " + index + ", size: 1");
	}

	public class Itr implements WeightedIterator<E> {

		public boolean done = false;

		@Override
		public boolean hasNext() {
			return !this.done;
		}

		@Override
		public E next() {
			if (this.done) throw new NoSuchElementException();
			this.done = true;
			return SingletonRandomList.this.element;
		}

		@Override
		public double getWeight() {
			if (!this.done) throw new IllegalStateException();
			return SingletonRandomList.this.weight;
		}
	}

	public class ListItr implements WeightedListIterator<E> {

		public static final byte SKIPPED = 1;
		public static final byte RETURNED = 2;

		public byte flags = 0;

		@Override
		public boolean hasNext() {
			return (this.flags & SKIPPED) == 0;
		}

		@Override
		public E next() {
			if ((this.flags & SKIPPED) != 0) throw new NoSuchElementException();
			this.flags = SKIPPED | RETURNED;
			return SingletonRandomList.this.element;
		}

		@Override
		public int nextIndex() {
			return (this.flags & SKIPPED) == 0 ? 0 : 1;
		}

		@Override
		public boolean hasPrevious() {
			return (this.flags & SKIPPED) != 0;
		}

		@Override
		public E previous() {
			if ((this.flags & SKIPPED) == 0) throw new NoSuchElementException();
			this.flags = RETURNED;
			return SingletonRandomList.this.element;
		}

		@Override
		public int previousIndex() {
			return (this.flags & SKIPPED) == 0 ? -1 : 0;
		}

		@Override
		public double getWeight() {
			if ((this.flags & RETURNED) == 0) throw new IllegalStateException();
			return SingletonRandomList.this.weight;
		}

		@Override
		public void set(E e) {
			if ((this.flags & RETURNED) == 0) throw new IllegalStateException();
			SingletonRandomList.this.element = e;
		}

		@Override
		public void setWeight(double weight) {
			if ((this.flags & RETURNED) == 0) throw new IllegalStateException();
			SingletonRandomList.this.weight = weight;
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