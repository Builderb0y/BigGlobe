package builderb0y.bigglobe.randomLists;

import java.util.function.Function;
import java.util.random.RandomGenerator;

public abstract class MappingRandomList<T_From, T_To> extends AbstractRandomList<T_To> {

	public final IRandomList<T_From> delegate;

	public MappingRandomList(IRandomList<T_From> delegate) {
		this.delegate = delegate;
	}

	public static <T_From, T_To> MappingRandomList<T_From, T_To> create(IRandomList<T_From> list, Function<? super T_From, ? extends T_To> mapper) {
		return new MappingRandomList<>(list) {

			@Override
			public T_To map(T_From from) {
				return mapper.apply(from);
			}
		};
	}

	public abstract T_To map(T_From from);

	@Override
	public T_To get(int index) {
		return this.map(this.delegate.get(index));
	}

	@Override
	public double getWeight(int index) {
		return this.delegate.getWeight(index);
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public int getRandomIndex(RandomGenerator random) {
		return this.delegate.getRandomIndex(random);
	}

	@Override
	public int getRandomIndex(long seed) {
		return this.delegate.getRandomIndex(seed);
	}
}