package builderb0y.bigglobe.randomLists;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ContainedRandomList<E extends IWeightedListElement> extends ComputedRandomList<E> {

	public ContainedRandomList() {}

	public ContainedRandomList(int initialCapacity) {
		super(initialCapacity);
	}

	public static <C extends IWeightedListElement> Collector<C, ?, ContainedRandomList<C>> collector() {
		return Collectors.toCollection(ContainedRandomList::new);
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