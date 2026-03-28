package builderb0y.bigglobe.scripting.wrappers;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;

import builderb0y.autocodec.annotations.Wrapper;

/**
wraps an array as a List, while also implementing RandomAccess.
implementing RandomAccess is important because scripts can use
index-based iteration instead of Iterator-based iteration for them.
{@link Arrays#asList(Object[])} simply returns a List,
and while the runtime type implements RandomAccess,
the compile time type does not. so that method is not useful for scripts.
*/
@Wrapper
public class ArrayWrapper<T> extends AbstractList<T> implements RandomAccess {

	public final T[] elements;

	@SafeVarargs
	public ArrayWrapper(T... elements) {
		this.elements = elements;
	}

	@Override
	public T get(int index) {
		return this.elements[index];
	}

	@Override
	public int size() {
		return this.elements.length;
	}
}