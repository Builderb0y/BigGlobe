package builderb0y.bigglobe.randomLists;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.PrimitiveCoders;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.AutoDecoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.AutoEncoder;
import builderb0y.autocodec.encoders.AutoEncoder.NamedEncoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.imprinters.AutoImprinter.NamedImprinter;
import builderb0y.autocodec.imprinters.ImprintContext;
import builderb0y.autocodec.imprinters.ImprintException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.randomLists.IRandomList.RandomAccessKnownTotalWeightRandomList;

/**
this implementation stores the elements and weights separately, in parallel arrays.
all add() methods which do NOT specify the weight will throw an UnsupportedOperationException.
*/
@UseImprinter(name = "new", in = RandomList.RandomListImprinter.class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
@UseEncoder  (name = "new", in = RandomList.RandomListEncoder  .class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
public class RandomList<E> extends AbstractRandomList<E> implements RandomAccessKnownTotalWeightRandomList<E>, Cloneable {

	public Object[] elements;
	public double[] weights;
	public int size = 0;
	public double totalWeight = 0;

	public RandomList() {
		this.elements = ObjectArrays.DEFAULT_EMPTY_ARRAY;
		this.weights  = DoubleArrays.DEFAULT_EMPTY_ARRAY;
	}

	public RandomList(int initialCapacity) {
		this.elements = initialCapacity == 0 ? ObjectArrays.DEFAULT_EMPTY_ARRAY : new Object[initialCapacity];
		this.weights  = initialCapacity == 0 ? DoubleArrays.DEFAULT_EMPTY_ARRAY : new double[initialCapacity];
	}

	public RandomList(RandomList<? extends E> other) {
		if (other.isEmpty()) {
			this.elements = ObjectArrays.DEFAULT_EMPTY_ARRAY;
			this.weights  = DoubleArrays.DEFAULT_EMPTY_ARRAY;
		}
		else {
			this.elements    = Arrays.copyOf(other.elements, other.size);
			this.weights     = Arrays.copyOf(other.weights,  other.size);
			this.size        = other.size;
			this.totalWeight = other.totalWeight;
		}
	}

	@SuppressWarnings("unchecked")
	public final E getRawElement(int index) {
		return (E)(this.elements[index]);
	}

	public final void setRawElement(int index, E element) {
		this.elements[index] = element;
	}

	@SuppressWarnings("unchecked")
	public final E[] castRawElements() {
		return (E[])(this.elements);
	}

	public static double checkWeight(double weight) {
		if (Double.isNaN(weight)) return 0.0D;
		if (weight >= 0.0D) return weight;
		throw new IllegalArgumentException("weight must be greater than or equal to 0.0.");
	}

	//////////////////////////////// get ////////////////////////////////

	@Override
	public E get(int index) {
		this.checkIndex(index);
		return this.getRawElement(index);
	}

	@Override
	public E getRandomElement(RandomGenerator random) {
		return RandomAccessKnownTotalWeightRandomList.super.getRandomElement(random);
	}

	@Override
	public E getRandomElement(long seed) {
		return RandomAccessKnownTotalWeightRandomList.super.getRandomElement(seed);
	}

	@Override
	public double getWeight(int index) {
		this.checkIndex(index);
		return this.weights[index];
	}

	@Override
	public double getTotalWeight() {
		return this.totalWeight;
	}

	//////////////////////////////// set ////////////////////////////////

	@Override
	public E set(int index, E element) {
		this.checkIndex(index);
		E oldValue = this.getRawElement(index);
		this.setRawElement(index, element);
		return oldValue;
	}

	@Override
	public E set(int index, E element, double weight) {
		this.checkIndex(index);
		checkWeight(weight);
		E oldValue = this.getRawElement(index);
		double oldWeight = this.weights[index];
		this.setRawElement(index, element);
		this.weights[index] = weight;
		this.totalWeight += weight - oldWeight;
		return oldValue;
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		this.modCount++;
		E[] elements = this.castRawElements();
		for (int index = 0, size = this.size; index < size; index++) {
			elements[index] = operator.apply(elements[index]);
		}
	}

	@Override
	public double setWeight(int index, double weight) {
		this.checkIndex(index);
		double oldWeight = this.weights[index];
		this.weights[index] = weight;
		return oldWeight;
	}

	@Override
	public void replaceAllWeights(ToDoubleFunction<? super E> updater) {
		int size = this.size;
		if (size == 0) return;
		E[] elements = this.castRawElements();
		double[] weights = this.weights;
		double totalWeight = 0.0D;
		int index = 0;
		try {
			for (; index < size; index++) {
				totalWeight += (weights[index] = checkWeight(updater.applyAsDouble(elements[index])));
			}
		}
		finally {
			for (; index < size; index++) {
				totalWeight += weights[index];
			}
			this.totalWeight = totalWeight;
		}
	}

	//////////////////////////////// contains ////////////////////////////////

	@Override
	public int indexOf(Object element) {
		E[] elements = this.castRawElements();
		if (element != null) {
			for (int i = 0, size = this.size; i < size; i++) {
				if (element.equals(elements[i])) return i;
			}
		}
		else {
			for (int i = 0, size = this.size; i < size; i++) {
				if (elements[i] == null) return i;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object element) {
		E[] elements = this.castRawElements();
		if (element != null) {
			for (int i = this.size; i-- != 0;) {
				if (element.equals(elements[i])) return i;
			}
		}
		else {
			for (int i = this.size; i-- != 0;) {
				if (elements[i] == null) return i;
			}
		}
		return -1;
	}

	//////////////////////////////// size/empty ////////////////////////////////

	@Override
	public int size() {
		return this.size;
	}

	//////////////////////////////// add ////////////////////////////////

	@Override
	@Deprecated
	public boolean add(E e) {
		throw new UnsupportedOperationException("Must specify weight");
	}

	@Override
	public boolean add(E element, double weight) {
		checkWeight(weight);
		this.modCount++;
		int size = this.size, newSize = size + 1;
		this.ensureCapacity(newSize);
		this.elements[size] = element;
		this.weights[size] = weight;
		this.size = newSize;
		this.totalWeight += weight;
		return true;
	}

	//builder-style add method.
	//adds the element, and returns this list.
	public RandomList<E> addSelf(E element, double weight) {
		this.add(element, weight);
		return this;
	}

	@Override
	@Deprecated
	public void add(int index, E element) {
		throw new UnsupportedOperationException("Must specify weight");
	}

	@Override
	public void add(int index, E element, double weight) {
		this.checkIndexForAdd(index);
		checkWeight(weight);
		this.modCount++;
		int size = this.size, newSize = size + 1;
		this.ensureCapacity(newSize);
		int moved = size - index;
		if (moved != 0) {
			System.arraycopy(this.elements, index, this.elements, index + 1, moved);
			System.arraycopy(this.weights, index, this.weights, index + 1, moved);
		}
		this.elements[index] = element;
		this.weights[index] = weight;
		this.size = newSize;
		this.totalWeight += weight;
	}

	@Override
	@Deprecated
	public boolean addAll(Collection<? extends E> c) {
		return this.addAll(this.size, c);
	}

	@Override
	@Deprecated
	public boolean addAll(int index, Collection<? extends E> c) {
		if (c instanceof IRandomList<?>) {
			return this.addAll(index, (IRandomList<? extends E>)(c));
		}
		else {
			throw new UnsupportedOperationException("Added collection must be an IRandomList too, so that we know what weights its elements have.");
		}
	}

	public boolean addAll(IRandomList<? extends E> c) {
		return this.addAll(this.size, c);
	}

	public boolean addAll(int index, IRandomList<? extends E> c) {
		this.checkIndexForAdd(index);
		if (c.isEmpty()) return false;
		this.modCount++;
		int cSize = c.size();
		this.ensureCapacity(this.size + cSize);
		if (index != this.size) {
			System.arraycopy(this.elements, index, this.elements, index + cSize, this.size - index);
			System.arraycopy(this.weights, index, this.weights, index + cSize, this.size - index);
		}
		double cTotalWeight;
		if (c instanceof RandomList<?>) {
			RandomList<? extends E> r = (RandomList<? extends E>)(c);
			System.arraycopy(r.elements, 0, this.elements, index, cSize);
			System.arraycopy(r.weights, 0, this.weights, index, cSize);
			cTotalWeight = r.totalWeight;
		}
		else {
			cTotalWeight = 0.0D;
			for (WeightedIterator<? extends E> iterator = c.iterator(); iterator.hasNext();) {
				this.elements[index] = iterator.next();
				cTotalWeight += (this.weights[index] = iterator.getWeight());
				index++;
			}
		}
		this.size += cSize;
		this.totalWeight += cTotalWeight;
		return true;
	}

	//////////////////////////////// remove ////////////////////////////////

	@Override
	public E remove(int index) {
		this.checkIndex(index);
		this.modCount++;
		E oldValue = this.getRawElement(index);
		double oldWeight = this.weights[index];
		int moved = this.size - index - 1;
		if (moved != 0) {
			System.arraycopy(this.elements, index + 1, this.elements, index, moved);
			System.arraycopy(this.weights,  index + 1, this.weights,  index, moved);
		}
		this.elements[--this.size] = null; //clear element for GC
		//don't need to clear weights, because doubles aren't GC'd.
		this.totalWeight -= oldWeight;
		return oldValue;
	}

	@Override
	public boolean remove(Object o) {
		int index = this.indexOf(o);
		if (index < 0) return false;
		this.remove(index); //will increment modCount.
		return true;
	}

	@Override
	public void removeRange(int fromIndex, int toIndex) {
		this.checkBoundsForSubList(fromIndex, toIndex);
		this.modCount++;
		double[] weights = this.weights;

		double totalWeight = this.totalWeight;
		for (int i = fromIndex; i < toIndex; i++) totalWeight -= weights[i];
		this.totalWeight = totalWeight;

		int moved = this.size - toIndex;
		if (moved != 0) {
			System.arraycopy(this.elements, toIndex, this.elements, fromIndex, moved);
			System.arraycopy(weights, toIndex, weights, fromIndex, moved);
		}
		int newSize = this.size - (toIndex - fromIndex);
		Arrays.fill(this.elements, newSize, this.size, null);
		this.size = newSize;
	}

	@Override
	public void clear() {
		if (this.isEmpty()) return;
		this.modCount++;
		Arrays.fill(this.elements, 0, this.size, null);
		this.size = 0;
		this.totalWeight = 0.0D;
	}

	//////////////////////////////// iterators ////////////////////////////////

	@Override
	public Spliterator<E> spliterator() {
		return Spliterators.spliterator(this.elements, 0, this.size, Spliterator.ORDERED);
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		E[] elements = this.castRawElements();
		int modCount = this.modCount;
		for (int i = 0, size = this.size; i < size; i++) {
			action.accept(elements[i]);
			if (modCount != this.modCount) throw new ConcurrentModificationException();
		}
	}

	//////////////////////////////// view ////////////////////////////////

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(this.elements, this.size, Object[].class);
	}

	@Override
	@SuppressWarnings({ "unchecked", "SuspiciousSystemArraycopy" })
	public <T1> T1[] toArray(T1[] a) {
		int size = this.size;
		if (a.length < size) a = (T1[])(Array.newInstance(a.getClass().getComponentType(), size));
		System.arraycopy(this.elements, 0, a, 0, size);
		if (a.length > size) a[size] = null;
		return a;
	}

	//////////////////////////////// other ////////////////////////////////

	public void ensureCapacity(int minCapacity) {
		if (minCapacity <= this.elements.length) return;
		int capacity = this.elements.length << 1;
		if (capacity < 0) capacity = Integer.MAX_VALUE - 8; //max array size as specified by ArrayList
		if (capacity < minCapacity) capacity = minCapacity;
		this.elements = Arrays.copyOf(this.elements, capacity);
		this.weights = Arrays.copyOf(this.weights, capacity);
	}

	public void trimToSize() {
		int size = this.size;
		if (size < this.elements.length) {
			if (size == 0) {
				this.elements = ObjectArrays.EMPTY_ARRAY;
				this.weights  = DoubleArrays.EMPTY_ARRAY;
			}
			else {
				this.elements = Arrays.copyOf(this.elements, size);
				this.weights  = Arrays.copyOf(this.weights,  size);
			}
		}
	}

	public Swapper swapper() {
		Object[] elements = this.elements;
		double[] weights = this.weights;
		return (index1, index2) -> {
			ObjectArrays.swap(elements, index1, index2);
			DoubleArrays.swap(weights,  index1, index2);
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public void sort(Comparator<? super E> comparator) {
		if (this.size <= 1) return;
		this.modCount++;
		E[] elements = this.castRawElements();
		it.unimi.dsi.fastutil.Arrays.quickSort(
			0,
			this.size,
			comparator != null
			? (index1, index2) -> comparator.compare(elements[index1], elements[index2])
			: (index1, index2) -> ((Comparable<Object>)(elements[index1])).compareTo(elements[index2]),
			this.swapper()
		);
	}

	//largestWeightFirst is helpful when several getRandomElement() are expected
	//to follow, since a descending order will make such calls faster on average.
	public void sortByWeight(boolean largestWeightFirst) {
		if (this.size <= 1) return;
		this.modCount++;
		double[] weights = this.weights;
		it.unimi.dsi.fastutil.Arrays.quickSort(
			0,
			this.size,
			largestWeightFirst
			? (index1, index2) -> Double.compare(weights[index2], weights[index1])
			: (index1, index2) -> Double.compare(weights[index1], weights[index2]),
			this.swapper()
		);
	}

	@Override
	public RandomList<E> clone() {
		try {
			@SuppressWarnings("unchecked")
			RandomList<E> clone = (RandomList<E>)(super.clone());
			clone.elements = Arrays.copyOf(clone.elements, clone.size);
			clone.weights = Arrays.copyOf(clone.weights, clone.size);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e);
		}
	}

	//////////////////////////////// coding ////////////////////////////////

	public static @Nullable String elementName(ReifiedType<?> elementType) {
		UseName useName = elementType.getAnnotations().getFirst(UseName.class);
		if (useName != null) return useName.value();
		if (elementType.getAnnotations().has(EncodeInline.class)) return null;
		return "element";
	}

	public static class RandomListImprinter<T> extends NamedImprinter<RandomList<T>> {

		public static final AutoDecoder<Double> WEIGHT_DECODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(
			new ReifiedType<@VerifyFloatRange(min = 0.0D, minInclusive = false, max = Double.POSITIVE_INFINITY, maxInclusive = false) Double>() {}
		);

		public final @NotNull AutoDecoder<T> elementDecoder;
		public final @Nullable String elementName;
		public final boolean allowSingleton;

		public RandomListImprinter(
			@NotNull ReifiedType<RandomList<T>> type,
			@NotNull AutoDecoder<T> elementDecoder,
			@Nullable String elementName,
			boolean allowSingleton
		) {
			super(type);
			this.elementDecoder = elementDecoder;
			this.elementName    = elementName;
			this.allowSingleton = allowSingleton;
		}

		public RandomListImprinter(FactoryContext<RandomList<T>> context) {
			super(context.type);
			@SuppressWarnings("unchecked")
			ReifiedType<T> elementType = (ReifiedType<T>)(context.type.resolveParameter(RandomList.class));
			this.elementDecoder = context.type(elementType).forceCreateDecoder();
			this.elementName = elementName(elementType);
			this.allowSingleton = elementType.getAnnotations().has(SingletonArray.class);
		}

		@Override
		public <T_Encoded> void imprint(@NotNull ImprintContext<T_Encoded, RandomList<T>> context) throws ImprintException {
			try {
				RandomList<T> result = context.object;
				for (DecodeContext<T_Encoded> entry : context.forceAsList(this.allowSingleton)) {
					result.add(
						(this.elementName != null ? entry.getMember(this.elementName) : entry).decodeWith(this.elementDecoder),
						entry.getMember("weight").decodeWith(WEIGHT_DECODER)
					);
				}
			}
			catch (ImprintException exception) {
				throw exception;
			}
			catch (DecodeException exception) {
				throw new ImprintException(exception);
			}
		}
	}

	public static class RandomListEncoder<T> extends NamedEncoder<RandomList<T>> {

		public final @NotNull AutoEncoder<T> elementEncoder;
		public final @Nullable String elementName;
		public final boolean singletonArray;

		public RandomListEncoder(
			@NotNull ReifiedType<RandomList<T>> type,
			@NotNull AutoEncoder<T> elementEncoder,
			@Nullable String elementName,
			boolean singletonArray
		) {
			super(type);
			this.elementEncoder = elementEncoder;
			this.elementName = elementName;
			this.singletonArray = singletonArray;
		}

		public RandomListEncoder(FactoryContext<RandomList<T>> context) {
			super(context.type);
			@SuppressWarnings("unchecked")
			ReifiedType<T> elementType = (ReifiedType<T>)(context.type.resolveParameter(RandomList.class));
			this.elementEncoder = context.type(elementType).forceCreateEncoder();
			this.elementName = elementName(elementType);
			this.singletonArray = elementType.getAnnotations().has(SingletonArray.class);
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RandomList<T>> context) throws EncodeException {
			RandomList<T> list = context.input;
			if (list == null) return context.empty();
			if (this.singletonArray && list.size() == 1) {
				return this.encodeElement(context, list.get(0), list.getWeight(0));
			}
			return context.createList(
				IntStream
				.range(0, list.size())
				.mapToObj((int index) -> {
					return this.encodeElement(context, list.get(index), list.getWeight(index));
				})
			);
		}

		public <T_Encoded> @NotNull T_Encoded encodeElement(EncodeContext<T_Encoded, RandomList<T>> context, T element, double weight) {
			T_Encoded encodedElement = context.input(element).encodeWith(this.elementEncoder);
			T_Encoded encodedWeight  = context.input(weight).encodeWith(PrimitiveCoders.DOUBLE);
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
		}
	}
}