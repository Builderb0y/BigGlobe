package builderb0y.bigglobe.randomLists;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;

import builderb0y.bigglobe.randomLists.IRandomList.RandomAccessKnownTotalWeightRandomList;

/**
an extension of {@link ContainedRandomList} which can
be used if the weight of any given element is constant.
usually, this implies that the backing
field storing the element's weight is final.
if this condition is met, then this implementation
can offer better performance characteristics in the
{@link #getRandomElement(RandomGenerator)} methods,
by utilizing the algorithm outlined in
{@link RandomAccessKnownTotalWeightRandomList}.
*/
public class ConstantContainedRandomList<E extends IWeightedListElement> extends ContainedRandomList<E> implements RandomAccessKnownTotalWeightRandomList<E> {

	public double totalWeight;

	public ConstantContainedRandomList() {}

	public ConstantContainedRandomList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public E set(int index, E element) {
		E old = super.set(index, element);
		this.totalWeight += element.getWeight() - old.getWeight();
		return old;
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		try {
			super.replaceAll(operator);
		}
		finally {
			double totalWeight = 0.0D;
			for (int index = 0, size = this.size(); index < size; index++) {
				totalWeight += this.getRawElement(index).getWeight();
			}
			this.totalWeight = totalWeight;
		}
	}

	@Override
	public boolean add(E element) {
		super.add(element);
		this.totalWeight += element.getWeight();
		return true;
	}

	@Override
	public void add(int index, E element) {
		super.add(index, element);
		this.totalWeight += element.getWeight();
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		if (super.addAll(collection)) {
			if (collection instanceof KnownTotalWeightRandomList<? extends E> list) {
				this.totalWeight += list.getTotalWeight();
			}
			else {
				double totalWeight = this.totalWeight;
				for (E element : collection) {
					totalWeight += element.getWeight();
				}
				this.totalWeight = totalWeight;
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> collection) {
		if (super.addAll(index, collection)) {
			if (collection instanceof KnownTotalWeightRandomList<? extends E> list) {
				this.totalWeight += list.getTotalWeight();
			}
			else {
				double totalWeight = this.totalWeight;
				for (E element : collection) {
					totalWeight += element.getWeight();
				}
				this.totalWeight = totalWeight;
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public E remove(int index) {
		E removed = super.remove(index);
		this.totalWeight -= removed.getWeight();
		return removed;
	}

	@Override
	public boolean remove(Object toRemove) {
		if (super.remove(toRemove)) {
			this.totalWeight -= ((IWeightedListElement)(toRemove)).getWeight();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		Objects.requireNonNull(filter, "filter");
		E[] elements = this.castRawElements();
		int readIndex = 0, writeIndex = 0, size = this.size;
		double totalWeight = this.totalWeight;
		try {
			for (; readIndex < size; readIndex++) {
				E element = elements[readIndex];
				if (!filter.test(element)) {
					totalWeight -= element.getWeight();
					elements[writeIndex++] = element;
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
			this.totalWeight = totalWeight;
		}
		return this.size != size;
	}

	@Override
	public void removeRange(int fromIndex, int toIndex) {
		this.checkBoundsForSubList(fromIndex, toIndex);
		double totalWeight = this.totalWeight;
		for (int index = fromIndex; index < toIndex; index++) {
			totalWeight -= this.getRawElement(index).getWeight();
		}
		this.totalWeight = totalWeight;
		super.removeRange(fromIndex, toIndex);
	}

	@Override
	public void clear() {
		super.clear();
		this.totalWeight = 0.0D;
	}

	@Override
	public ConstantContainedRandomList<E> clone() {
		return (ConstantContainedRandomList<E>)(super.clone());
	}

	@Override
	public double getTotalWeight() {
		return this.totalWeight;
	}

	@Override
	public E getRandomElement(RandomGenerator random) {
		return RandomAccessKnownTotalWeightRandomList.super.getRandomElement(random);
	}

	@Override
	public E getRandomElement(long seed) {
		return RandomAccessKnownTotalWeightRandomList.super.getRandomElement(seed);
	}
}