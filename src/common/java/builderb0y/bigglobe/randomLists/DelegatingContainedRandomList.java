package builderb0y.bigglobe.randomLists;

import java.util.List;
import java.util.RandomAccess;

public class DelegatingContainedRandomList<E extends IWeightedListElement> extends AbstractRandomList<E> {

	public final List<E> delegate;

	public DelegatingContainedRandomList(List<E> delegate) {
		this.delegate = delegate;
	}

	public static <E extends IWeightedListElement> DelegatingContainedRandomList<E> from(List<E> delegate) {
		return delegate instanceof RandomAccess ? new RandomAccessDelegatingContainedRandomList<>(delegate) : new DelegatingContainedRandomList<>(delegate);
	}

	@Override
	public E get(int index) {
		return this.delegate.get(index);
	}

	@Override
	public double getWeight(int index) {
		return this.delegate.get(index).getWeight();
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	public static class RandomAccessDelegatingContainedRandomList<E extends IWeightedListElement> extends DelegatingContainedRandomList<E> implements RandomAccessRandomList<E> {

		public RandomAccessDelegatingContainedRandomList(List<E> delegate) {
			super(delegate);
		}
	}
}