package builderb0y.bigglobe.randomLists;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.PrimitiveCoders;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.AutoDecoder;
import builderb0y.autocodec.decoders.AutoDecoder.NamedDecoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.AutoEncoder;
import builderb0y.autocodec.encoders.AutoEncoder.NamedEncoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.noise.Permuter;

/**
a list which can return random elements via {@link #getRandomElement(RandomGenerator)}.
additionally, all elements have a "weight" which can make
them more or less likely to be returned by {@link #getRandomElement(RandomGenerator)}.
all sub-classes are expected to document their policy
for how elements or weights may be added or modified.
some modification methods may throw an
UnsupportedOperationException if this policy is not satisfied.
*/
@UseDecoder(name = "new", in = IRandomList.IRandomListDecoder.class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
@UseEncoder(name = "new", in = IRandomList.IRandomListEncoder.class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
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
	returns an IRandomList which might be a specialized class
	that holds exactly the number of elements this list contains.
	the returned list cannot be assumed to change if
	this list is modified after this method returns.
	more generally, it's probably not a good idea to
	modify this list after calling this method.
	attempts to do so may result in undefined behavior or
	inconsistent states, and generally be difficult to debug.
	*/
	public default IRandomList<E> optimizeSize() {
		return switch (this.size()) {
			case 0 -> EmptyRandomList.instance();
			case 1 -> new SingletonRandomList<>(this.get(0), this.getWeight(0));
			default -> this;
		};
	}

	/**
	similar to the above method, but static.
	if the provided list is null, this method behaves as if it were empty.
	*/
	public static <E> IRandomList<E> optimizeSizeNullable(IRandomList<E> list) {
		return list != null ? list.optimizeSize() : EmptyRandomList.instance();
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
			}
			return this.get(this.size() - 1);
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
			}
			return this.get(this.size() - 1);
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
			}
			return this.get(this.size() - 1);
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
			}
			return this.get(this.size() - 1);
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

	public static class IRandomListDecoder<T> extends NamedDecoder<IRandomList<T>> {

		public static final AutoDecoder<Double> WEIGHT_DECODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(
			new ReifiedType<@VerifyFloatRange(min = 0.0D, minInclusive = false, max = Float.POSITIVE_INFINITY, maxInclusive = false) Double>() {}
		);

		public final @NotNull AutoDecoder<T> elementDecoder;
		public final @Nullable String elementName;

		public IRandomListDecoder(
			@NotNull ReifiedType<IRandomList<T>> type,
			@NotNull AutoDecoder<T> elementDecoder,
			@Nullable String elementName
		) {
			super(type);
			this.elementDecoder = elementDecoder;
			this.elementName = elementName;
		}

		public IRandomListDecoder(FactoryContext<IRandomList<T>> context) {
			super(context.type);
			@SuppressWarnings("unchecked")
			ReifiedType<T> elementType = (ReifiedType<T>)(context.type.resolveParameter(IRandomList.class));
			this.elementDecoder = context.type(elementType).forceCreateDecoder();
			this.elementName = elementName(elementType);
		}

		public <T_Encoded> T element(DecodeContext<T_Encoded> context) throws DecodeException {
			return (this.elementName != null ? context.getMember(this.elementName) : context).decodeWith(this.elementDecoder);
		}

		public <T_Encoded> double weight(DecodeContext<T_Encoded> context) throws DecodeException {
			return context.getMember("weight").decodeWith(WEIGHT_DECODER);
		}

		@Override
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
						yield result;
					}
				};
			}
			else {
				//assume singleton.
				return new SingletonRandomList<>(context.decodeWith(this.elementDecoder), DEFAULT_WEIGHT);
			}
		}
	}

	public static class IRandomListEncoder<T> extends NamedEncoder<IRandomList<T>> {

		public final @NotNull AutoEncoder<T> elementEncoder;
		public final @Nullable String elementName;

		public IRandomListEncoder(
			@NotNull ReifiedType<IRandomList<T>> type,
			@NotNull AutoEncoder<T> elementEncoder,
			@Nullable String elementName
		) {
			super(type);
			this.elementEncoder = elementEncoder;
			this.elementName = elementName;
		}

		public IRandomListEncoder(FactoryContext<IRandomList<T>> context) {
			super(context.type);
			@SuppressWarnings("unchecked")
			ReifiedType<T> elementType = (ReifiedType<T>)(context.type.resolveParameter(IRandomList.class));
			this.elementEncoder = context.type(elementType).forceCreateEncoder();
			this.elementName = elementName(elementType);
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, IRandomList<T>> context) throws EncodeException {
			IRandomList<T> input = context.input;
			if (input == null) return context.empty();
			return switch (input.size()) {
				case 0 -> context.emptyList();
				case 1 -> context.input(input.get(0)).encodeWith(this.elementEncoder);
				default -> context.createList(
					IntStream.range(0, input.size()).mapToObj((int index) -> {
						T_Encoded encodedElement = context.input(input.get(index)).encodeWith(this.elementEncoder);
						T_Encoded encodedWeight  = context.input(input.getWeight(index)).encodeWith(PrimitiveCoders.DOUBLE);
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
			};
		}
	}
}