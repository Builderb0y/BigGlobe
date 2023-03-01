package builderb0y.scripting.util;

import java.util.ArrayList;
import java.util.function.IntFunction;

public class ArrayBuilder<T> extends ArrayList<T> {

	public ArrayBuilder<T> append(T element) {
		this.add(element);
		return this;
	}

	public ArrayBuilder<T> append(T element, int count) {
		for (int loop = 0; loop < count; loop++) {
			this.add(element);
		}
		return this;
	}

	@Override
	public <T1> T1[] toArray(IntFunction<T1[]> generator) {
		return this.toArray(generator.apply(this.size()));
	}
}