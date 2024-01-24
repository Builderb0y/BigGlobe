package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Array;

public class MappedRangeObjectArray<T> extends MappedRangeArray {

	public T[] array;

	public MappedRangeObjectArray(T[] array) {
		this.array = array;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void reallocate(int requiredLength) {
		this.valid = true;
		if (this.array.length < requiredLength) {
			requiredLength = Math.max(requiredLength, this.array.length << 1);
			this.array = (T[])(Array.newInstance(this.array.getClass().getComponentType(), requiredLength));
		}
	}
}