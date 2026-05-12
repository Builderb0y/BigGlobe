package builderb0y.bigglobe.util;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
{@link Collections#checkedList(List, Class)}, but with null checks.
can't extend the class that that method returns because it's private.
just another reason why private stuff sucks I guess.
*/
public class CheckedList<E> implements List<E>, RandomAccess {

	public final List<E> delegate;
	public final Class<E> type;
	public final NullPolicy nullPolicy;

	public CheckedList(List<E> delegate, Class<E> type, NullPolicy policy) {
		this.delegate = delegate;
		this.type = type;
		this.nullPolicy = policy;
	}

	public CheckedList(int initialCapacity, Class<E> type, NullPolicy policy) {
		this(new ArrayList<>(initialCapacity), type, policy);
	}

	public CheckedList(Class<E> type, NullPolicy policy) {
		this(new ArrayList<>(), type, policy);
	}

	public boolean check(Object object) {
		if (this.type.isInstance(object)) {
			return true;
		}
		else if (object == null) {
			switch (this.nullPolicy) {
				case ADD -> { return true; }
				case THROW -> { /* fallthrough */ }
				case IGNORE -> { return false; }
			}
		}
		throw new IllegalArgumentException("Attempt to add " + object + " to a List of type " + this.type.getName());
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public boolean contains(Object object) {
		return this.delegate.contains(object);
	}

	@Override
	public Iterator<E> iterator() {
		return this.new CheckedIterator(this.delegate.iterator());
	}

	@Override
	public Object[] toArray() {
		return this.delegate.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return this.delegate.toArray(array);
	}

	@Override
	public boolean add(E element) {
		return this.check(element) && this.delegate.add(element);
	}

	@Override
	public boolean remove(Object object) {
		return this.delegate.remove(object);
	}

	@Override
	@SuppressWarnings("SlowListContainsAll")
	public boolean containsAll(Collection<?> collection) {
		if (this.delegate.size() > 2 && collection.size() > 2) {
			return new HashSet<>(this.delegate).containsAll(collection);
		}
		else {
			return this.delegate.containsAll(collection);
		}
	}

	@Override
	@SuppressWarnings("NonShortCircuitBooleanExpression")
	public boolean addAll(Collection<? extends E> collection) {
		boolean changed = false;
		for (E element : collection) {
			if (this.check(element) && this.delegate.add(element)) {
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> collection) {
		if (collection.isEmpty()) return false;
		List<E> filtered = new ArrayList<>(collection.size());
		for (E element : collection) {
			if (this.check(element)) filtered.add(element);
		}
		return this.delegate.addAll(index, filtered);
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		return this.delegate.removeAll(collection);
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return this.delegate.retainAll(collection);
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}

	@Override
	public E get(int index) {
		return this.delegate.get(index);
	}

	@Override
	public E set(int index, E element) {
		if (this.check(element)) {
			return this.delegate.set(index, element);
		}
		else {
			//I... guess this is the most sane way to handle this,
			//given that I'm violating the contract of this method sometimes?
			return this.delegate.get(index);
		}
	}

	@Override
	public void add(int index, E element) {
		if (this.check(element)) this.delegate.add(index, element);
	}

	@Override
	public E remove(int index) {
		return this.delegate.remove(index);
	}

	@Override
	public int indexOf(Object object) {
		return this.delegate.indexOf(object);
	}

	@Override
	public int lastIndexOf(Object object) {
		return this.delegate.lastIndexOf(object);
	}

	@Override
	public ListIterator<E> listIterator() {
		return this.new CheckedListIterator(this.delegate.listIterator());
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return this.new CheckedListIterator(this.delegate.listIterator(index));
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new CheckedList<>(this.delegate.subList(fromIndex, toIndex), this.type, this.nullPolicy);
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		this.delegate.replaceAll((E element) -> {
			E replacement = operator.apply(element);
			return this.check(replacement) ? replacement : element;
		});
	}

	@Override
	public void sort(Comparator<? super E> comparator) {
		this.delegate.sort(comparator);
	}

	@Override
	public Spliterator<E> spliterator() {
		return this.delegate.spliterator();
	}

	@Override
	public <T> T[] toArray(IntFunction<T[]> generator) {
		return this.delegate.toArray(generator);
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
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object object) {
		return object == this || this.delegate.equals(object);
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

	public class CheckedIterator implements Iterator<E> {

		public final Iterator<E> delegate;

		public CheckedIterator(Iterator<E> delegate) {
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
		public void remove() {
			this.delegate.remove();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			this.delegate.forEachRemaining(action);
		}
	}

	public class CheckedListIterator implements ListIterator<E> {

		public final ListIterator<E> delegate;

		public CheckedListIterator(ListIterator<E> delegate) {
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
		public void set(E element) {
			if (CheckedList.this.check(element)) {
				this.delegate.set(element);
			}
		}

		@Override
		public void add(E element) {
			if (CheckedList.this.check(element)) {
				this.delegate.add(element);
			}
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			this.delegate.forEachRemaining(action);
		}
	}

	public static enum NullPolicy {
		ADD,
		THROW,
		IGNORE;
	}
}