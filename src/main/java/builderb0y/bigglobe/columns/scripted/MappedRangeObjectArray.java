package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Array;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MappedRangeObjectArray<T> extends MappedRangeArray {

	public static final TypeInfo TYPE = type(MappedRangeObjectArray.class);
	public static final FieldInfo ARRAY = FieldInfo.inCaller("array");
	public static final MethodInfo CONSTRUCTOR = MethodInfo.getConstructor(MappedRangeObjectArray.class);

	public T[] array;

	public MappedRangeObjectArray(T[] array) {
		this.array = array;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean reallocate(int requiredLength) {
		this.valid = true;
		if (this.array.length < requiredLength) {
			requiredLength = Math.max(requiredLength, this.array.length * 3 / 2);
			this.array = (T[])(Array.newInstance(this.array.getClass().getComponentType(), requiredLength));
		}
		return requiredLength > 0;
	}
}