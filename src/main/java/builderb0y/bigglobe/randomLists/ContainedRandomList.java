package builderb0y.bigglobe.randomLists;

/**
an extension of {@link ComputedRandomList} which enforces a standardized way of
getting the weight of each element, via the {@link IWeightedListElement} interface.
this implementation of {@link IRandomList} will only accept elements which implement this interface.
*/
public class ContainedRandomList<E extends IWeightedListElement> extends ComputedRandomList<E> {

	public ContainedRandomList() {}

	public ContainedRandomList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public double getWeightOfElement(E element) {
		return element.getWeight();
	}

	@Override
	public ContainedRandomList<E> clone() {
		return (ContainedRandomList<E>)(super.clone());
	}
}