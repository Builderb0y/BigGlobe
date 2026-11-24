package builderb0y.bigglobe.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class MappedSet<T_Internal, T_External> extends AbstractSet<T_External> {

	public final Set<T_Internal> internalSet;
	public final Function<T_Internal, T_External> internalToExternal;
	public final Function<T_External, T_Internal> externalToInternal;

	public MappedSet(
		Set<T_Internal> internalSet,
		Function<T_Internal, T_External> internalToExternal,
		Function<T_External, T_Internal> externalToInternal
	) {
		this.internalSet = internalSet;
		this.internalToExternal = internalToExternal;
		this.externalToInternal = externalToInternal;
	}

	@Override
	public boolean add(T_External external) {
		return this.internalSet.add(this.externalToInternal.apply(external));
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean contains(Object object) {
		return this.internalSet.contains(this.externalToInternal.apply((T_External)(object)));
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object object) {
		return this.internalSet.remove(this.externalToInternal.apply((T_External)(object)));
	}

	@Override
	public void clear() {
		this.internalSet.clear();
	}

	@Override
	public Stream<T_External> stream() {
		return this.internalSet.stream().map(this.internalToExternal);
	}

	@Override
	public Stream<T_External> parallelStream() {
		return this.internalSet.parallelStream().map(this.internalToExternal);
	}

	@Override
	public Iterator<T_External> iterator() {
		Iterator<T_Internal> internalIterator = this.internalSet.iterator();
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return internalIterator.hasNext();
			}

			@Override
			public T_External next() {
				return MappedSet.this.internalToExternal.apply(internalIterator.next());
			}

			@Override
			public void remove() {
				internalIterator.remove();
			}
		};
	}

	@Override
	public int size() {
		return this.internalSet.size();
	}
}