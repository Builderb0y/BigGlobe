package builderb0y.bigglobe.randomLists;

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