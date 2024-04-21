package builderb0y.bigglobe.randomLists;

import java.util.random.RandomGenerator;

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
public class ConstantContainedRandomList<E extends IWeightedListElement> extends ConstantComputedRandomList<E> {

	public ConstantContainedRandomList() {}

	public ConstantContainedRandomList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public double getWeightOfElement(E element) {
		return element.getWeight();
	}

	@Override
	public ConstantContainedRandomList<E> clone() {
		return (ConstantContainedRandomList<E>)(super.clone());
	}
}