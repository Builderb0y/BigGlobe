package builderb0y.bigglobe.scripting.wrappers;

import java.util.random.RandomGenerator;

public interface TagWrapper<T> extends Iterable<T> {

	public abstract T random(RandomGenerator random);
}