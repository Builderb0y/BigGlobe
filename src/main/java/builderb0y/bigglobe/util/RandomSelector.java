package builderb0y.bigglobe.util;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

public class RandomSelector<T> {

	public final RandomGenerator random;
	public T value;
	public double currentChance;

	public RandomSelector(RandomGenerator random) {
		this.random = random;
	}

	public void accept(T newValue, double newChance) {
		if (newChance > 0.0D && this.random.nextDouble(this.currentChance += newChance) < newChance) {
			this.value = newValue;
		}
	}

	public void accept(RandomSelector<? extends T> that) {
		this.accept(that.value, that.currentChance);
	}

	public static <T_Merged, T_Element> Collector<T_Merged, RandomSelector<T_Element>, T_Element> collector(
		RandomGenerator random,
		Function<? super T_Merged, ? extends T_Element> elementGetter,
		ToDoubleFunction<? super T_Merged> weightGetter
	) {
		return Collector.of(
			() -> new RandomSelector<>(random),
			(RandomSelector<T_Element> selector, T_Merged merged) -> selector.accept(elementGetter.apply(merged), weightGetter.applyAsDouble(merged)),
			(RandomSelector<T_Element> selector1, RandomSelector<T_Element> selector2) -> { selector1.accept(selector2); return selector1; },
			(RandomSelector<T_Element> selector) -> selector.value,
			Characteristics.UNORDERED
		);
	}

	public static <T_Element> Collector<T_Element, RandomSelector<T_Element>, T_Element> uniformWeightCollector(RandomGenerator random) {
		return collector(random, Function.identity(), (T_Element element) -> 1.0D);
	}
}