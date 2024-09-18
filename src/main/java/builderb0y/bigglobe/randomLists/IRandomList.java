package builderb0y.bigglobe.randomLists;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.coders.PrimitiveCoders;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.ConstantWeightRandomList.RandomAccessConstantWeightRandomList;

/**
a list which can return random elements via {@link #getRandomElement(RandomGenerator)}.
additionally, all elements have a "weight" which can make
them more or less likely to be returned by {@link #getRandomElement(RandomGenerator)}.
all sub-classes are expected to document their policy
for how elements or weights may be added or modified.
some modification methods may throw an
UnsupportedOperationException if this policy is not satisfied.
*/
@UseCoder(name = "new", in = IRandomList.IRandomListCoder.class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
public interface IRandomList<E> extends List<E> {

	public static final double DEFAULT_WEIGHT = 1.0D;

	public abstract double getWeight(int index);

	/** returns the old weight at this index, to be compliant with List.set(int, E). */
	public abstract double setWeight(int index, double weight);

	/** returns true, to be compliant with List.add(E). */
	public abstract boolean add(E element, double weight);

	/** returns void, to be compliant with List.add(int, E). */
	public abstract void add(int index, E element, double weight);

	/** returns the old element at this index, to be compliant with List.set(int, E). */
	public abstract E set(int index, E element, double weight);

	public default void replaceAllWeights(ToDoubleFunction<? super E> operator) {
		Objects.requireNonNull(operator, "operator");
		for (WeightedListIterator<E> iterator = this.listIterator(); iterator.hasNext();) {
			iterator.setWeight(operator.applyAsDouble(iterator.next()));
		}
	}

	public default E getRandomElement(RandomGenerator random) {
		if (this.isEmpty()) throw new NoSuchElementException();
		E choice = this.get(this.size() - 1);
		double totalWeight = 0.0D;
		for (WeightedIterator<E> iterator = this.iterator(); iterator.hasNext(); ) {
			E element = iterator.next();
			double weight = iterator.getWeight();
			if (weight > 0.0D && random.nextDouble() * (totalWeight += weight) < weight) {
				choice = element;
			}
		}
		if (!(totalWeight > 0.0D)) choice = null;
		return choice;
	}

	public default E getRandomElement(long seed) {
		if (this.isEmpty()) throw new NoSuchElementException();
		E choice = this.get(this.size() - 1);
		double totalWeight = 0.0D;
		for (WeightedIterator<E> iterator = this.iterator(); iterator.hasNext(); ) {
			E element = iterator.next();
			double weight = iterator.getWeight();
			if (weight > 0.0D && Permuter.nextPositiveDouble(seed += Permuter.PHI64) * (totalWeight += weight) < weight) {
				choice = element;
			}
		}
		if (!(totalWeight > 0.0D)) choice = null;
		return choice;
	}

	@Override
	public abstract WeightedIterator<E> iterator();

	@Override
	public abstract WeightedListIterator<E> listIterator();

	@Override
	public abstract WeightedListIterator<E> listIterator(int index);

	@Override
	public abstract IRandomList<E> subList(int fromIndex, int toIndex);

	/**
	returns an IRandomList which contains the same elements and
	weights as this IRandomList, but is possibly faster to query.
	no guarantees are made about the properties of the returned
	IRandomList. in particular, the returned IRandomList might
	be immutable. it might also be independent from this list
	such that changes to this list are NOT reflected in the
	returned list and vise versa. more generally, it is not
	recommended to modify this list or the returned list
	while you still have a live reference to the other list.
	if no better/faster-to-query representation of this list
	is possible, this list is returned as-is.
	*/
	public default IRandomList<E> optimize() {
		return switch (this.size()) {
			case 0 -> EmptyRandomList.instance();
			case 1 -> new SingletonRandomList<>(this.get(0), this.getWeight(0));
			default -> {
				double weight;
				if (this instanceof RandomAccess) {
					weight = this.getWeight(0);
					for (int index = 1, size = this.size(); index < size; index++) {
						if (this.getWeight(index) != weight) {
							yield this;
						}
					}
					yield new RandomAccessConstantWeightRandomList<>(this, weight);
				}
				else {
					WeightedIterator<E> iterator = this.iterator();
					iterator.next();
					weight = iterator.getWeight();
					while (iterator.hasNext()) {
						iterator.next();
						if (iterator.getWeight() != weight) {
							yield this;
						}
					}
					yield new RandomAccessConstantWeightRandomList<>(new ArrayList<>(this), weight);
				}
			}
		};
	}

	/**
	similar to the above method, but static.
	if the provided list is null, this method returns {@link EmptyRandomList#instance()}.
	*/
	public static <E> IRandomList<E> optimizeSizeNullable(IRandomList<E> list) {
		return list != null ? list.optimize() : EmptyRandomList.instance();
	}

	public default String defaultToString() {
		int size = this.size();
		if (size == 0) return "[]";
		StringBuilder builder = new StringBuilder(size << 6);
		WeightedIterator<E> iterator = this.iterator();
		builder.append("[ ").append(iterator.next()).append(" * ").append(iterator.getWeight());
		while (iterator.hasNext()) {
			builder.append(", ").append(iterator.next()).append(" * ").append(iterator.getWeight());
		}
		return builder.append(" ]").toString();
	}

	public default boolean defaultEquals(Object o) {
		if (this == o) return true;
		if (!(o instanceof IRandomList<?>)) return false;
		IRandomList<?> that = (IRandomList<?>)(o);
		if (this.size() != that.size()) return false;
		WeightedIterator<E> thisIterator = this.iterator();
		if (that instanceof RandomAccess) {
			for (int index = 0, size = that.size(); index < size; index++) {
				if (!thisIterator.hasNext()) return false;
				if (!Objects.equals(thisIterator.next(), that.get(index))) return false;
				if (Double.doubleToLongBits(thisIterator.getWeight()) != Double.doubleToLongBits(that.getWeight(index))) return false;
			}
			return !thisIterator.hasNext();
		}
		else {
			WeightedIterator<?> thatIterator = that.iterator();
			while (thisIterator.hasNext() && thatIterator.hasNext()) {
				if (!Objects.equals(thisIterator.next(), thatIterator.next())) return false;
				if (Double.doubleToLongBits(thisIterator.getWeight()) != Double.doubleToLongBits(thatIterator.getWeight())) return false;
			}
			return !thisIterator.hasNext() && !thatIterator.hasNext(); //handle concurrent lists.
		}
	}

	public default int defaultHashCode() {
		int hash = 1;
		for (WeightedIterator<E> iterator = this.iterator(); iterator.hasNext();) {
			hash = hash * 31 + Objects.hashCode(iterator.next());
			hash = hash * 31 + Double.hashCode(iterator.getWeight());
		}
		return hash;
	}

	public static interface WeightedIterator<E> extends Iterator<E> {

		/**
		returns the weight of the previous element returned by next().
		throws IllegalStateException if next() has not been called yet,
		of if remove() has been called after the previous invocation of next().
		*/
		public abstract double getWeight();
	}

	public static interface WeightedListIterator<E> extends WeightedIterator<E>, ListIterator<E> {

		public abstract void setWeight(double weight);
	}

	public static interface RandomAccessRandomList<E> extends IRandomList<E>, RandomAccess {

		@Override
		public default void replaceAllWeights(ToDoubleFunction<? super E> operator) {
			Objects.requireNonNull(operator, "operator");
			for (int index = 0, size = this.size(); index < size; index++) {
				this.setWeight(index, operator.applyAsDouble(this.get(index)));
			}
		}

		@Override
		public default E getRandomElement(RandomGenerator random) {
			if (this.isEmpty()) throw new NoSuchElementException();
			//this algorithm takes advantage of RandomAccess
			//by not instantiating a WeightedIterator.
			E choice = this.get(this.size() - 1);
			double totalWeight = 0.0D;
			for (int index = 0, size = this.size(); index < size; index++) {
				double weight = this.getWeight(index);
				if (weight > 0.0D && random.nextDouble() * (totalWeight += weight) < weight) {
					choice = this.get(index);
				}
			}
			if (!(totalWeight > 0.0D)) choice = null;
			return choice;
		}

		@Override
		public default E getRandomElement(long seed) {
			if (this.isEmpty()) throw new NoSuchElementException();
			//this algorithm takes advantage of RandomAccess
			//by not instantiating a WeightedIterator.
			E choice = this.get(this.size() - 1);
			double totalWeight = 0.0D;
			for (int index = 0, size = this.size(); index < size; index++) {
				double weight = this.getWeight(index);
				if (weight > 0.0D && Permuter.nextPositiveDouble(seed += Permuter.PHI64) * (totalWeight += weight) < weight) {
					choice = this.get(index);
				}
			}
			if (!(totalWeight > 0.0D)) choice = null;
			return choice;
		}

		@Override
		public default String defaultToString() {
			int size = this.size();
			if (size == 0) return "[]";
			StringBuilder builder = new StringBuilder(size << 6);
			builder.append("[ ").append(this.get(0)).append(" * ").append(this.getWeight(0));
			for (int index = 1; index < size; index++) {
				builder.append(", ").append(this.get(index)).append(" * ").append(this.getWeight(index));
			}
			return builder.append(" ]").toString();
		}

		@Override
		public default boolean defaultEquals(Object o) {
			if (this == o) return true;
			if (!(o instanceof IRandomList<?>)) return false;
			IRandomList<?> that = (IRandomList<?>)(o);
			if (this.size() != that.size()) return false;
			if (that instanceof RandomAccess) {
				for (int index = 0, size = this.size(); index < size; index++) {
					if (!Objects.equals(this.get(index), that.get(index))) return false;
					if (Double.doubleToLongBits(this.getWeight(index)) != Double.doubleToLongBits(that.getWeight(index))) return false;
				}
				return true;
			}
			else {
				WeightedIterator<?> thatIterator = that.iterator();
				for (int index = 0, size = this.size(); index < size; index++) {
					if (!thatIterator.hasNext()) return false;
					if (!Objects.equals(this.get(index), thatIterator.next())) return false;
					if (Double.doubleToLongBits(this.getWeight(index)) != Double.doubleToLongBits(thatIterator.getWeight())) return false;
				}
				return !thatIterator.hasNext(); //handle concurrent lists.
			}
		}

		@Override
		public default int defaultHashCode() {
			int hash = 1;
			for (int index = 0, size = this.size(); index < size; index++) {
				hash = hash * 31 + Objects.hashCode(this.get(index));
				hash = hash * 31 + Double.hashCode(this.getWeight(index));
			}
			return hash;
		}
	}

	public static interface KnownTotalWeightRandomList<E> extends IRandomList<E> {

		/** returns the sum of the weights of all elements in this list. */
		public abstract double getTotalWeight();

		public default boolean isWeightless() {
			return !(this.getTotalWeight() > 0.0D);
		}

		public default boolean isEmptyOrWeightless() {
			return this.isEmpty() || this.isWeightless();
		}

		@Override
		public default E getRandomElement(RandomGenerator random) {
			if (this.isEmpty()) throw new NoSuchElementException();
			//this algorithm takes advantage of knowing the total weight by only
			//invoking random.nextDouble() once, instead of once per element.
			//additionally, it might stop iterating before the end of the list is reached.
			double targetWeight = this.getTotalWeight();
			if (targetWeight > 0.0D) {
				targetWeight *= random.nextDouble();
				for (WeightedIterator<E> iterator = this.iterator(); iterator.hasNext(); ) {
					E element = iterator.next();
					double weight = iterator.getWeight();
					if (weight > 0.0D && (targetWeight -= weight) <= 0.0D) {
						return element;
					}
				}
				return this.get(this.size() - 1);
			}
			return null;
		}

		@Override
		public default E getRandomElement(long seed) {
			if (this.isEmpty()) throw new NoSuchElementException();
			//this algorithm takes advantage of knowing the total weight by only
			//invoking Permuter.nextPositiveDouble() once, instead of once per element.
			//additionally, it might stop iterating before the end of the list is reached.
			double targetWeight = this.getTotalWeight();
			if (targetWeight > 0.0D) {
				targetWeight *= Permuter.nextPositiveDouble(seed);
				for (WeightedIterator<E> iterator = this.iterator(); iterator.hasNext(); ) {
					E element = iterator.next();
					double weight = iterator.getWeight();
					if (weight > 0.0D && (targetWeight -= weight) <= 0.0D) {
						return element;
					}
				}
				return this.get(this.size() - 1);
			}
			return null;
		}

		@Override
		public default String defaultToString() {
			return IRandomList.super.defaultToString() + " (total weight: " + this.getTotalWeight() + ')';
		}
	}

	public static interface RandomAccessKnownTotalWeightRandomList<E> extends KnownTotalWeightRandomList<E>, RandomAccessRandomList<E> {

		@Override
		public default E getRandomElement(RandomGenerator random) {
			if (this.isEmpty()) throw new NoSuchElementException();
			//this algorithm is a hybrid of RandomAccessRandomList's
			//algorithm and KnownTotalWeightRandomList's algorithm.
			double targetWeight = this.getTotalWeight();
			if (targetWeight > 0.0D) {
				targetWeight *= random.nextDouble();
				for (int index = 0, size = this.size(); index < size; index++) {
					double weight = this.getWeight(index);
					if (weight > 0.0D && (targetWeight -= weight) <= 0.0D) {
						return this.get(index);
					}
				}
				return this.get(this.size() - 1);
			}
			return null;
		}

		@Override
		public default E getRandomElement(long seed) {
			if (this.isEmpty()) throw new NoSuchElementException();
			//this algorithm is a hybrid of RandomAccessRandomList's
			//algorithm and KnownTotalWeightRandomList's algorithm.
			double targetWeight = this.getTotalWeight();
			if (targetWeight > 0.0D) {
				targetWeight *= Permuter.nextPositiveDouble(seed);
				for (int index = 0, size = this.size(); index < size; index++) {
					double weight = this.getWeight(index);
					if (weight > 0.0D && (targetWeight -= weight) <= 0.0D) {
						return this.get(index);
					}
				}
				return this.get(this.size() - 1);
			}
			return null;
		}

		@Override
		public default String defaultToString() {
			return RandomAccessRandomList.super.defaultToString() + " (total weight: " + this.getTotalWeight() + ')';
		}
	}



	//////////////////////////////// coding ////////////////////////////////



	public static @Nullable String elementName(ReifiedType<?> elementType) {
		UseName useName = elementType.getAnnotations().getFirst(UseName.class);
		if (useName != null) return useName.value();
		if (elementType.getAnnotations().has(EncodeInline.class)) return null;
		return "element";
	}

	public static class IRandomListCoder<T> extends NamedCoder<IRandomList<T>> {

		public final @NotNull AutoCoder<T> elementCoder;
		public final @NotNull AutoCoder<Double> weightCoder;
		public final @Nullable String elementName;

		public IRandomListCoder(
			@NotNull ReifiedType<IRandomList<T>> handledType,
			@NotNull AutoCoder<T> elementCoder,
			@NotNull AutoCoder<Double> weightCoder,
			@Nullable String elementName
		) {
			super(handledType);
			this.elementCoder = elementCoder;
			this.weightCoder = weightCoder;
			this.elementName = elementName;
		}

		public IRandomListCoder(FactoryContext<IRandomList<T>> context) {
			super(context.type);
			ReifiedType<T> elementType = context.type.resolveParameter(IRandomList.class).uncheckedCast();
			this.elementCoder = context.type(elementType).forceCreateCoder();
			this.elementName = elementName(elementType);
			this.weightCoder = context.type(new ReifiedType<@VerifyFloatRange(min = 0.0D, minInclusive = false, max = Float.POSITIVE_INFINITY, maxInclusive = false) Double>() {}).forceCreateCoder();
		}

		public <T_Encoded> T element(DecodeContext<T_Encoded> context) throws DecodeException {
			return (this.elementName != null && context.isMap() ? context.getMember(this.elementName) : context).decodeWith(this.elementCoder);
		}

		public <T_Encoded> double weight(DecodeContext<T_Encoded> context) throws DecodeException {
			return context.isMap() ? context.getMember("weight").decodeWith(this.weightCoder) : DEFAULT_WEIGHT;
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @Nullable IRandomList<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			List<DecodeContext<T_Encoded>> list = context.tryAsList(false);
			if (list != null) {
				return switch (list.size()) {
					case 0 -> EmptyRandomList.instance();
					case 1 -> {
						DecodeContext<T_Encoded> entry = list.get(0);
						yield new SingletonRandomList<>(this.element(entry), this.weight(entry));
					}
					default -> {
						RandomList<T> result = new RandomList<>(list.size());
						for (DecodeContext<T_Encoded> entry : list) {
							result.add(this.element(entry), this.weight(entry));
						}
						yield result.optimize();
					}
				};
			}
			else {
				//assume singleton.
				return new SingletonRandomList<>(context.decodeWith(this.elementCoder), DEFAULT_WEIGHT);
			}
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, IRandomList<T>> context) throws EncodeException {
			IRandomList<T> list = context.object;
			if (list == null) return context.empty();
			if (list.isEmpty()) return context.emptyList();
			return context.createList(
				IntStream.range(0, list.size()).mapToObj((int index) -> {
					T_Encoded encodedElement = context.object(list.get(index)).encodeWith(this.elementCoder);
					T_Encoded encodedWeight  = context.object(list.getWeight(index)).encodeWith(PrimitiveCoders.DOUBLE);
					if (this.elementName != null) {
						return context.createStringMap(
							Map.of(
								this.elementName, encodedElement,
								"weight", encodedWeight
							)
						);
					}
					else {
						return context.addToStringMap(encodedElement, "weight", encodedWeight);
					}
				})
			);
		}
	}
}