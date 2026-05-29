package builderb0y.bigglobe.util;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import builderb0y.bigglobe.util.Grouper.KeyGetter.KeyPerElementGetter;
import builderb0y.bigglobe.util.Grouper.KeyGetter.KeyPerValueGetter;
import builderb0y.bigglobe.util.Grouper.KeyGetter.KeysPerElementGetter;
import builderb0y.bigglobe.util.Grouper.KeyGetter.KeysPerValueGetter;
import builderb0y.bigglobe.util.Grouper.ValueGetter.ValuePerKeyGetter;
import builderb0y.bigglobe.util.Grouper.ValueGetter.ValuesPerElementGetter;
import builderb0y.bigglobe.util.Grouper.ValueGetter.ValuesPerKeyGetter;
import builderb0y.bigglobe.util.Grouper.ValueGetter.ValuePerElementGetter;

public class Grouper<
	T_Element,
	T_Key,
	T_Value,
	T_Values,
	T_Map extends Map<T_Key, T_Values>
>
implements Collector<T_Element, T_Map, T_Map> {

	public static final Set<Characteristics> CHARACTERISTICS = Collections.singleton(Characteristics.IDENTITY_FINISH);
	public static final int
		/**
		if set, when the {@link #valueGetter} produces more than one value,
		the values will first be collected into a
		{@link T_Values} via our {@link #downstream}
		before being combined with whatever was in the map
		previously via our downstream's {@link Collector#combiner()}.

		if not set, when the {@link #valueGetter} produces more than one value,
		the values will be accumulated onto whatever was there previously
		via our downstream's {@link Collector#accumulator()}.

		this flag has no effect ifour {@link #valueGetter} only produces one value per element.
		*/
		FLAG_ACCUMULATE_IN_BULK = 1 << 0;

	public final Supplier<T_Map> mapSupplier;
	public final KeyGetter<T_Element, T_Key, T_Value> keyGetter;
	public final ValueGetter<T_Element, T_Key, T_Value> valueGetter;
	public final Collector<T_Value, T_Values, T_Values> downstream;
	public final int flags;

	public Grouper(
		Supplier<T_Map> mapSupplier,
		KeyGetter<T_Element, T_Key, T_Value> keyGetter,
		ValueGetter<T_Element, T_Key, T_Value> valueGetter,
		Collector<T_Value, T_Values, T_Values> downstream,
		int flags
	) {
		this.mapSupplier = Objects.requireNonNull(mapSupplier, "mapSupplier");
		this.keyGetter   = Objects.requireNonNull(keyGetter,   "keyGetter"  );
		this.valueGetter = Objects.requireNonNull(valueGetter, "valueGetter");
		this.downstream  = Objects.requireNonNull(downstream,  "downstream" );
		this.flags       = flags;
	}

	@SuppressWarnings("unchecked")
	public static <T> Stream<T> castStream(Stream<? extends T> stream) {
		return (Stream<T>)(stream);
	}

	@SuppressWarnings("unchecked")
	public static <T_From, T_To> Stream<T_To> filterByClass(Stream<T_From> stream, Class<T_To> clazz) {
		return (Stream<T_To>)(stream.filter(clazz::isInstance));
	}

	//*sigh*
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T_Element> Collector<T_Element, List<T_Element>, List<T_Element>> toList() {
		return (Collector)(Collectors.toList());
	}

	//*sigh*
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T_Element> Collector<T_Element, Set<T_Element>, Set<T_Element>> toSet() {
		return (Collector)(Collectors.toList());
	}

	//*sigh*
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T_Element, T_Collection extends Collection<T_Element>>
	Collector<T_Element, T_Collection, T_Collection> toCollection(Supplier<? extends T_Collection> supplier) {
		return (Collector)(Collectors.toCollection(supplier));
	}

	public static <T_Element, T_Key, T_Value>
	Grouper<T_Element, T_Key, T_Value, List<T_Value>, Map<T_Key, List<T_Value>>> groupingToList(
		KeyGetter<T_Element, T_Key, T_Value> keyGetter,
		ValueGetter<T_Element, T_Key, T_Value> valueGetter
	) {
		return new Grouper<>(HashMap::new, keyGetter, valueGetter, toList(), 0);
	}

	public static <T_Element, T_Key, T_Value>
	Grouper<T_Element, T_Key, T_Value, Set<T_Value>, Map<T_Key, Set<T_Value>>> groupingToSet(
		KeyGetter<T_Element, T_Key, T_Value> keyGetter,
		ValueGetter<T_Element, T_Key, T_Value> valueGetter
	) {
		return new Grouper<>(HashMap::new, keyGetter, valueGetter, toSet(), 0);
	}

	public static <T_Key, T_Values, T_Map extends Map<T_Key, T_Values>>
	Grouper<Map<T_Key, T_Values>, T_Key, T_Values, T_Values, T_Map> mergingMaps(
		Supplier<T_Map> mapSupplier,
		Collector<T_Values, T_Values, T_Values> downstream
	) {
		return new Grouper<>(
			mapSupplier,
			keysPerElement((Map<T_Key, T_Values> map) -> map.keySet().stream()),
			valuePerKey(Map::get),
			downstream,
			0
		);
	}

	//////////////////////////////// util ////////////////////////////////

	public boolean hasAnyFlags(int flags) {
		return (this.flags & flags) != 0;
	}

	public boolean hasAllFlags(int flags) {
		return (this.flags & flags) == flags;
	}

	public Grouper<T_Element, T_Key, T_Value, T_Values, T_Map> withFlags(int flags) {
		return this.hasAllFlags(flags) ? this : new Grouper<>(this.mapSupplier, this.keyGetter, this.valueGetter, this.downstream, this.flags | flags);
	}

	public Grouper<T_Element, T_Key, T_Value, T_Values, T_Map> withoutFlags(int flags) {
		return !this.hasAnyFlags(flags) ? this : new Grouper<>(this.mapSupplier, this.keyGetter, this.valueGetter, this.downstream, this.flags & ~flags);
	}

	public <T_NewMap extends Map<T_Key, T_Values>> Grouper<T_Element, T_Key, T_Value, T_Values, T_NewMap> withMapSupplier(Supplier<T_NewMap> supplier) {
		return new Grouper<>(supplier, this.keyGetter, this.valueGetter, this.downstream, this.flags);
	}

	@Override
	public Supplier<T_Map> supplier() {
		return this.mapSupplier;
	}

	public T_Values values(T_Map map, T_Key key) {
		return map.computeIfAbsent(key, (T_Key _) -> this.downstream.supplier().get());
	}

	public void mergeValue(T_Values map, T_Value value) {
		this.downstream.accumulator().accept(map, value);
	}

	public void mergeValues(T_Map map, T_Key key, Stream<? extends T_Value> newValues) {
		if (this.hasAnyFlags(FLAG_ACCUMULATE_IN_BULK)) {
			map.merge(key, newValues.collect(this.downstream), this.downstream.combiner());
		}
		else {
			T_Values oldValues = this.values(map, key);
			newValues.forEach((T_Value value) -> this.mergeValue(oldValues, value));
		}
	}

	@Override
	public BiConsumer<T_Map, T_Element> accumulator() {
		return switch (this.keyGetter) {
			case KeyGetter.KeyPerElementGetter<T_Element, T_Key, T_Value> keyGetter -> switch (this.valueGetter) {
				case ValueGetter.ValuePerElementGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					T_Key key = keyGetter.getKey(element);
					T_Value value = valueGetter.getValue(element);
					this.mergeValue(this.values(map, key), value);
				};
				case ValueGetter.ValuePerKeyGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					T_Key key = keyGetter.getKey(element);
					T_Value value = valueGetter.getValue(element, key);
					this.mergeValue(this.values(map, key), value);
				};
				case ValueGetter.ValuesPerElementGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					this.mergeValues(map, keyGetter.getKey(element), valueGetter.getValues(element));
				};
				case ValueGetter.ValuesPerKeyGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					T_Key key = keyGetter.getKey(element);
					this.mergeValues(map, key, valueGetter.getValues(element, key));
				};
			};
			case KeyGetter.KeysPerElementGetter<T_Element, T_Key, T_Value> keyGetter -> switch (this.valueGetter) {
				case ValueGetter.ValuePerElementGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					T_Value value = valueGetter.getValue(element);
					keyGetter.getKeys(element).forEach((T_Key key) -> this.mergeValue(this.values(map, key), value));
				};
				case ValueGetter.ValuePerKeyGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					keyGetter.getKeys(element).forEach((T_Key key) -> this.mergeValue(this.values(map, key), valueGetter.getValue(element, key)));
				};
				case ValueGetter.ValuesPerElementGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					keyGetter.getKeys(element).forEach((T_Key key) -> {
						this.mergeValues(map, key, valueGetter.getValues(element));
					});
				};
				case ValueGetter.ValuesPerKeyGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					keyGetter.getKeys(element).forEach((T_Key key) -> {
						this.mergeValues(map, key, valueGetter.getValues(element, key));
					});
				};
			};
			case KeyGetter.KeyPerValueGetter<T_Element, T_Key, T_Value> keyGetter -> switch (this.valueGetter) {
				case ValueGetter.ValuePerElementGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					T_Value value = valueGetter.getValue(element);
					T_Key key = keyGetter.getKey(element, value);
					this.mergeValue(this.values(map, key), value);
				};
				case ValueGetter.ValuePerKeyGetter<T_Element, T_Key, T_Value> valueGetter -> {
					throw new IllegalStateException("Mutual dependence between keys and values");
				}
				case ValueGetter.ValuesPerElementGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					valueGetter.getValues(element).forEach((T_Value value) -> {
						T_Key key = keyGetter.getKey(element, value);
						this.mergeValue(this.values(map, key), value);
					});
				};
				case ValueGetter.ValuesPerKeyGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					throw new IllegalStateException("Mutual dependence between keys and values");
				};
			};
			case KeyGetter.KeysPerValueGetter<T_Element, T_Key, T_Value> keyGetter -> switch (this.valueGetter) {
				case ValueGetter.ValuePerElementGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					T_Value value = valueGetter.getValue(element);
					keyGetter.getKeys(element, value).forEach((T_Key key) -> {
						this.mergeValue(this.values(map, key), value);
					});
				};
				case ValueGetter.ValuePerKeyGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					throw new IllegalStateException("Mutual dependence between keys and values");
				};
				case ValueGetter.ValuesPerElementGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					valueGetter.getValues(element).forEach((T_Value value) -> {
						keyGetter.getKeys(element, value).forEach((T_Key key) -> {
							this.mergeValue(this.values(map, key), value);
						});
					});
				};
				case ValueGetter.ValuesPerKeyGetter<T_Element, T_Key, T_Value> valueGetter -> (T_Map map, T_Element element) -> {
					throw new IllegalStateException("Mutual dependence between keys and values");
				};
			};
		};
	}

	@Override
	public BinaryOperator<T_Map> combiner() {
		return (T_Map map1, T_Map map2) -> {
			if (map1.size() < map2.size() && !this.downstream.characteristics().contains(Characteristics.UNORDERED)) {
				T_Map tmp = map1;
				map1 = map2;
				map2 = tmp;
			}
			if (map1 instanceof ConcurrentMap<?, ?>) {
				T_Map map1_ = map1;
				map2.entrySet().parallelStream().forEach((Map.Entry<T_Key, T_Values> entry) -> {
					map1_.merge(entry.getKey(), entry.getValue(), this.downstream.combiner());
				});
			}
			else {
				for (Map.Entry<T_Key, T_Values> entry : map2.entrySet()) {
					map1.merge(entry.getKey(), entry.getValue(), this.downstream.combiner());
				}
			}
			return map2;
		};
	}

	@Override
	public Function<T_Map, T_Map> finisher() {
		return Function.identity();
	}

	@Override
	public Set<Characteristics> characteristics() {
		return CHARACTERISTICS;
	}

	public static <T_Element, T_Key, T_Value> KeyPerElementGetter<T_Element, T_Key, T_Value> keyPerElement(KeyPerElementGetter<T_Element, T_Key, T_Value> policy) {
		return Objects.requireNonNull(policy, "policy");
	}

	public static <T_Element, T_Key, T_Value> KeyPerValueGetter<T_Element, T_Key, T_Value> keyPerValue(KeyPerValueGetter<T_Element, T_Key, T_Value> policy) {
		return Objects.requireNonNull(policy, "policy");
	}

	public static <T_Element, T_Key, T_Value> KeysPerElementGetter<T_Element, T_Key, T_Value> keysPerElement(KeysPerElementGetter<T_Element, T_Key, T_Value> policy) {
		return Objects.requireNonNull(policy, "policy");
	}

	public static <T_Element, T_Key, T_Value> KeysPerValueGetter<T_Element, T_Key, T_Value> keysPerValue(KeysPerValueGetter<T_Element, T_Key, T_Value> policy) {
		return Objects.requireNonNull(policy, "policy");
	}

	public static <T_Element, T_Value> KeyPerElementGetter<T_Element, T_Element, T_Value> keyElement() {
		return (T_Element element) -> element;
	}

	public static <T_Element, T_Key, T_Value> ValuePerElementGetter<T_Element, T_Key, T_Value> valuePerElement(ValuePerElementGetter<T_Element, T_Key, T_Value> policy) {
		return Objects.requireNonNull(policy, "policy");
	}

	public static <T_Element, T_Key, T_Value> ValuePerKeyGetter<T_Element, T_Key, T_Value> valuePerKey(ValuePerKeyGetter<T_Element, T_Key, T_Value> policy) {
		return Objects.requireNonNull(policy, "policy");
	}

	public static <T_Element, T_Key, T_Value> ValuesPerElementGetter<T_Element, T_Key, T_Value> valuesPerElement(ValuesPerElementGetter<T_Element, T_Key, T_Value> policy) {
		return Objects.requireNonNull(policy, "policy");
	}

	public static <T_Element, T_Key, T_Value> ValuesPerKeyGetter<T_Element, T_Key, T_Value> valuesPerKey(ValuesPerKeyGetter<T_Element, T_Key, T_Value> policy) {
		return Objects.requireNonNull(policy, "policy");
	}

	public static <T_Element, T_Key> ValuePerElementGetter<T_Element, T_Key, T_Element> valueElement() {
		return (T_Element element) -> element;
	}

	public static sealed interface KeyGetter<T_Element, T_Key, T_Value> {

		@FunctionalInterface
		public static non-sealed interface KeyPerElementGetter<T_Element, T_Key, T_Value> extends KeyGetter<T_Element, T_Key, T_Value> {

			public abstract T_Key getKey(T_Element element);
		}

		@FunctionalInterface
		public static non-sealed interface KeyPerValueGetter<T_Element, T_Key, T_Value> extends KeyGetter<T_Element, T_Key, T_Value> {

			public abstract T_Key getKey(T_Element element, T_Value value);
		}

		@FunctionalInterface
		public static non-sealed interface KeysPerElementGetter<T_Element, T_Key, T_Value> extends KeyGetter<T_Element, T_Key, T_Value> {

			public abstract Stream<? extends T_Key> getKeys(T_Element element);
		}

		@FunctionalInterface
		public static non-sealed interface KeysPerValueGetter<T_Element, T_Key, T_Value> extends KeyGetter<T_Element, T_Key, T_Value> {

			public abstract Stream<? extends T_Key> getKeys(T_Element element, T_Value value);
		}
	}

	public static sealed interface ValueGetter<T_Element, T_Key, T_Value> {

		@FunctionalInterface
		public static non-sealed interface ValuePerElementGetter<T_Element, T_Key, T_Value> extends ValueGetter<T_Element, T_Key, T_Value> {

			public abstract T_Value getValue(T_Element element);
		}

		@FunctionalInterface
		public static non-sealed interface ValuePerKeyGetter<T_Element, T_Key, T_Value> extends ValueGetter<T_Element, T_Key, T_Value> {

			public abstract T_Value getValue(T_Element element, T_Key key);
		}

		@FunctionalInterface
		public static non-sealed interface ValuesPerElementGetter<T_Element, T_Key, T_Value> extends ValueGetter<T_Element, T_Key, T_Value> {

			public abstract Stream<? extends T_Value> getValues(T_Element element);
		}

		@FunctionalInterface
		public static non-sealed interface ValuesPerKeyGetter<T_Element, T_Key, T_Value> extends ValueGetter<T_Element, T_Key, T_Value> {

			public abstract Stream<? extends T_Value> getValues(T_Element element, T_Key value);
		}
	}
}