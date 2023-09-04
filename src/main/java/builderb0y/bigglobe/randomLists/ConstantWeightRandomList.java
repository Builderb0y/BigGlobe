package builderb0y.bigglobe.randomLists;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList.KnownTotalWeightRandomList;

/**
an IRandomList which does not store weights.
every element has the same weight.
specifically, IRandomList.DEFAULT_WEIGHT.
this class is intended to be a wrapper for another List.
*/
public class ConstantWeightRandomList<E> extends AbstractRandomList<E> implements KnownTotalWeightRandomList<E> {

	public final List<E> delegate;
	public final double weight;

	public ConstantWeightRandomList(List<E> delegate, double weight) {
		this.delegate = delegate;
		this.weight = weight;
	}

	@Override
	public double getWeight(int index) {
		this.checkIndex(index);
		return this.weight;
	}

	@Override
	public double getTotalWeight() {
		return this.size() * this.weight;
	}

	@Override
	public E get(int index) {
		return this.delegate.get(index);
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public boolean isWeightless() {
		return this.delegate.isEmpty();
	}

	@Override
	public boolean isEmptyOrWeightless() {
		return this.delegate.isEmpty();
	}

	@Override
	public E getRandomElement(RandomGenerator random) {
		return this.get(random.nextInt(this.size()));
	}

	@Override
	public E getRandomElement(long seed) {
		return this.get(Permuter.nextBoundedInt(seed, this.size()));
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		this.delegate.replaceAll(operator);
	}

	@Override
	public void sort(Comparator<? super E> c) {
		this.delegate.sort(c);
	}

	@Override
	public Spliterator<E> spliterator() {
		return this.delegate.spliterator();
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		return this.delegate.removeIf(filter);
	}

	@Override
	public Stream<E> stream() {
		return this.delegate.stream();
	}

	@Override
	public Stream<E> parallelStream() {
		return this.delegate.parallelStream();
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		this.delegate.forEach(action);
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ConstantWeightRandomList<?> that) {
			return this.delegate.equals(that.delegate) && this.weight == that.weight;
		}
		return this.defaultEquals(object);
	}

	@Override
	public IRandomList<E> subList(int fromIndex, int toIndex) {
		return new ConstantWeightRandomList<>(this.delegate.subList(fromIndex, toIndex), this.weight);
	}

	@Override
	public boolean add(E e) {
		return this.delegate.add(e);
	}

	@Override
	public E set(int index, E element) {
		return this.delegate.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		this.delegate.add(index, element);
	}

	@Override
	public E remove(int index) {
		return this.delegate.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return this.delegate.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.delegate.lastIndexOf(o);
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return this.delegate.addAll(index, c);
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.delegate.contains(o);
	}

	@NotNull
	@Override
	public Object[] toArray() {
		return this.delegate.toArray();
	}

	@NotNull
	@Override
	public <T> T[] toArray(@NotNull T[] a) {
		return this.delegate.toArray(a);
	}

	@Override
	public boolean remove(Object o) {
		return this.delegate.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.delegate.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return this.delegate.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.delegate.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.delegate.retainAll(c);
	}

	@Override
	public WeightedIterator<E> iterator() {
		return new WrappedIterator<>(this.delegate.iterator(), this.weight);
	}

	@Override
	public WeightedListIterator<E> listIterator() {
		return new WrappedListIterator<>(this.delegate.listIterator(), this.weight);
	}

	@Override
	public WeightedListIterator<E> listIterator(int index) {
		return new WrappedListIterator<>(this.delegate.listIterator(index), this.weight);
	}

	@Override
	public void replaceAllWeights(ToDoubleFunction<? super E> operator) {
		Objects.requireNonNull(operator, "operator");
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeRange(int fromIndex, int toIndex) {
		if (this.delegate instanceof AbstractList<?>) {
			this.delegate.subList(fromIndex, toIndex).clear();
		}
		else {
			super.removeRange(fromIndex, toIndex);
		}
	}

	public static class WrappedIterator<E> implements WeightedIterator<E> {

		public final Iterator<E> delegate;
		public final double weight;

		public WrappedIterator(Iterator<E> delegate, double weight) {
			this.delegate = delegate;
			this.weight = weight;
		}

		@Override
		public double getWeight() {
			return this.weight;
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
		public void remove() {
			this.delegate.remove();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			this.delegate.forEachRemaining(action);
		}
	}

	public static class WrappedListIterator<E> implements WeightedListIterator<E> {

		public final ListIterator<E> delegate;
		public final double weight;

		public WrappedListIterator(ListIterator<E> delegate, double weight) {
			this.delegate = delegate;
			this.weight = weight;
		}

		@Override
		public double getWeight() {
			return this.weight;
		}

		@Override
		public void setWeight(double weight) {
			throw new UnsupportedOperationException();
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
			this.delegate.remove();
		}

		@Override
		public void set(E e) {
			this.delegate.set(e);
		}

		@Override
		public void add(E e) {
			this.delegate.add(e);
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

	public static class RandomAccessConstantWeightRandomList<E> extends ConstantWeightRandomList<E> implements RandomAccessKnownTotalWeightRandomList<E> {

		public RandomAccessConstantWeightRandomList(List<E> delegate, double weight) {
			super(delegate, weight);
		}
	}
}