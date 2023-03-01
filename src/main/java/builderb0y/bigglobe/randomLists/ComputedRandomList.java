package builderb0y.bigglobe.randomLists;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.objects.ObjectArrays;

import builderb0y.bigglobe.randomLists.IRandomList.RandomAccessRandomList;

public abstract class ComputedRandomList<E> extends AbstractRandomList<E> implements RandomAccessRandomList<E>, Cloneable {

	public Object[] elements;
	public int size;

	public ComputedRandomList() {
		this.elements = ObjectArrays.DEFAULT_EMPTY_ARRAY;
	}

	public ComputedRandomList(int initialCapacity) {
		this.elements = initialCapacity == 0 ? ObjectArrays.DEFAULT_EMPTY_ARRAY : new Object[initialCapacity];
	}

	@SuppressWarnings("unchecked")
	public final E getRawElement(int index) {
		return (E)(this.elements[index]);
	}

	public final void setRawElement(int index, E element) {
		this.elements[index] = element;
	}

	@SuppressWarnings("unchecked")
	public final E[] castRawElements() {
		return (E[])(this.elements);
	}

	public abstract double getWeightOfElement(E element);

	//////////////////////////////// get ////////////////////////////////

	@Override
	public E get(int index) {
		this.checkIndex(index);
		return this.getRawElement(index);
	}

	@Override
	public double getWeight(int index) {
		return this.getWeightOfElement(this.get(index));
	}

	@Override
	public E getRandomElement(RandomGenerator random) {
		return RandomAccessRandomList.super.getRandomElement(random);
	}

	@Override
	public E getRandomElement(long seed) {
		return RandomAccessRandomList.super.getRandomElement(seed);
	}

	//////////////////////////////// set ////////////////////////////////

	@Override
	public E set(int index, E element) {
		this.checkIndex(index);
		this.modCount++;
		E oldValue = this.getRawElement(index);
		this.setRawElement(index, element);
		return oldValue;
	}

	@Override
	public E set(int index, E element, double weight) {
		return this.set(index, element);
	}

	@Override
	public double setWeight(int index, double weight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		this.modCount++;
		E[] elements = this.castRawElements();
		for (int index = 0, size = this.size; index < size; index++) {
			elements[index] = operator.apply(elements[index]);
		}
	}

	@Override
	public void replaceAllWeights(ToDoubleFunction<? super E> operator) {
		throw new UnsupportedOperationException();
	}

	//////////////////////////////// contains ////////////////////////////////

	@Override
	public int indexOf(Object element) {
		E[] elements = this.castRawElements();
		if (element != null) {
			for (int i = 0, size = this.size; i < size; i++) {
				if (element.equals(elements[i])) return i;
			}
		}
		else {
			for (int i = 0, size = this.size; i < size; i++) {
				if (elements[i] == null) return i;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object element) {
		E[] elements = this.castRawElements();
		if (element != null) {
			for (int i = this.size; i-- != 0;) {
				if (element.equals(elements[i])) return i;
			}
		}
		else {
			for (int i = this.size; i-- != 0;) {
				if (elements[i] == null) return i;
			}
		}
		return -1;
	}

	//////////////////////////////// size/empty ////////////////////////////////

	@Override
	public int size() {
		return this.size;
	}

	//////////////////////////////// add ////////////////////////////////

	@Override
	public boolean add(E e) {
		this.modCount++;
		int size = this.size, newSize = size + 1;
		this.ensureCapacity(newSize);
		this.elements[size] = e;
		this.size = newSize;
		return true;
	}

	@Override
	public void add(int index, E element) {
		this.checkIndexForAdd(index);
		this.modCount++;
		int size = this.size, newSize = size + 1;
		this.ensureCapacity(newSize);
		int moved = size - index;
		if (moved != 0) {
			System.arraycopy(this.elements, index, this.elements, index + 1, moved);
		}
		this.elements[index] = element;
		this.size = newSize;
	}

	@Override
	public boolean add(E element, double weight) {
		return this.add(element);
	}

	@Override
	public void add(int index, E element, double weight) {
		this.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (c.isEmpty()) return false;
		this.modCount++;
		Object[] newElements = c.toArray();
		this.ensureCapacity(this.size + newElements.length);
		System.arraycopy(newElements, 0, this.elements, this.size, newElements.length);
		this.size += newElements.length;
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		this.checkIndexForAdd(index);
		if (c.isEmpty()) return false;
		this.modCount++;
		Object[] newElements = c.toArray();
		this.ensureCapacity(this.size + newElements.length);
		if (index != this.size) {
			System.arraycopy(this.elements, index, this.elements, index + newElements.length, this.size - index);
		}
		System.arraycopy(newElements, 0, this.elements, index, newElements.length);
		this.size += newElements.length;
		return true;
	}

	//////////////////////////////// remove ////////////////////////////////

	@Override
	public E remove(int index) {
		this.checkIndex(index);
		this.modCount++;
		E[] elements = this.castRawElements();
		E oldValue = elements[index];
		int moved = this.size - index - 1;
		if (moved != 0) {
			System.arraycopy(elements, index + 1, elements, 0, moved);
		}
		elements[--this.size] = null; //clear element for GC.
		return oldValue;
	}

	@Override
	public boolean remove(Object o) {
		int index = this.indexOf(o);
		if (index < 0) return false;
		this.remove(index); //will increment modCount.
		return true;
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		Objects.requireNonNull(filter, "filter");
		E[] elements = this.castRawElements();
		int readIndex = 0, writeIndex = 0, size = this.size;
		try {
			for (; readIndex < size; readIndex++) {
				if (!filter.test(elements[readIndex])) {
					elements[writeIndex++] = elements[readIndex];
				}
			}
		}
		finally {
			//if the filter threw any exceptions, shift all remaining elements.
			while (readIndex < size) {
				elements[writeIndex++] = elements[readIndex++];
			}
			this.size = writeIndex;
			//clear removed indexes for GC.
			while (writeIndex < size) {
				elements[writeIndex++] = null;
			}
		}
		return this.size != size;
	}

	@Override
	public void removeRange(int fromIndex, int toIndex) {
		this.checkBoundsForSubList(fromIndex, toIndex);
		this.modCount++;
		int moved = this.size - toIndex;
		if (moved != 0) {
			System.arraycopy(this.elements, toIndex, this.elements, fromIndex, moved);
		}
		int newSize = this.size - (toIndex - fromIndex);
		Arrays.fill(this.elements, newSize, this.size, null);
		this.size = newSize;
	}

	@Override
	public void clear() {
		if (this.isEmpty()) return;
		this.modCount++;
		Arrays.fill(this.elements, 0, this.size, null);
		this.size = 0;
	}

	//////////////////////////////// iterators ////////////////////////////////

	@Override
	public Spliterator<E> spliterator() {
		return Spliterators.spliterator(this.elements, 0, this.size, Spliterator.ORDERED);
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		E[] elements = this.castRawElements();
		int modCount = this.modCount;
		for (int i = 0, size = this.size; i < size; i++) {
			action.accept(elements[i]);
			if (modCount != this.modCount) throw new ConcurrentModificationException();
		}
	}

	//////////////////////////////// view ////////////////////////////////

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(this.elements, this.size, Object[].class);
	}

	@Override
	@SuppressWarnings({ "unchecked", "SuspiciousSystemArraycopy" })
	public <T1> T1[] toArray(T1[] a) {
		int size = this.size;
		if (a.length < size) a = (T1[])(Array.newInstance(a.getClass().getComponentType(), size));
		System.arraycopy(this.elements, 0, a, 0, size);
		if (a.length > size) a[size] = null;
		return a;
	}

	//////////////////////////////// other ////////////////////////////////

	@Override
	public void sort(Comparator<? super E> c) {
		if (this.size > 1) {
			this.modCount++;
			Arrays.sort(this.castRawElements(), 0, this.size, c);
		}
	}

	public RandomList<E> cache() {
		int size = this.size;
		RandomList<E> list = new RandomList<>(size);
		for (int index = 0; index < size; index++) {
			E element = this.get(index);
			double weight = this.getWeightOfElement(element);
			if (weight > 0.0D) list.add(element, weight);
		}
		list.trimToSize();
		return list;
	}

	public void ensureCapacity(int minCapacity) {
		if (minCapacity <= this.elements.length) return;
		int capacity = this.elements.length << 1;
		if (capacity < 0) capacity = Integer.MAX_VALUE - 8; //probably overflowed. set to max array size as specified by ArrayList.
		if (capacity < minCapacity) capacity = minCapacity;
		this.elements = Arrays.copyOf(this.elements, capacity);
	}

	public void trimToSize() {
		int size = this.size;
		if (size < this.elements.length) {
			if (size == 0) {
				this.elements = ObjectArrays.EMPTY_ARRAY;
			}
			else {
				this.elements = Arrays.copyOf(this.elements, size);
			}
		}
	}

	@Override
	public ComputedRandomList<E> clone() {
		try {
			@SuppressWarnings("unchecked")
			ComputedRandomList<E> clone = (ComputedRandomList<E>)(super.clone());
			clone.elements = Arrays.copyOf(clone.elements, clone.size);
			return clone;
		}
		catch (CloneNotSupportedException exception) {
			throw new InternalError(exception);
		}
	}
}