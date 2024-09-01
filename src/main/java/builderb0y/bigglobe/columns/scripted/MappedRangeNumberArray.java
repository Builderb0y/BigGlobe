package builderb0y.bigglobe.columns.scripted;

import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MappedRangeNumberArray extends MappedRangeArray {

	public static final TypeInfo
		TYPE      = type(MappedRangeNumberArray.class);
	public static final FieldInfo
		ARRAY     =  FieldInfo.inCaller("array");
	public static final MethodInfo
		CONSTRUCT = MethodInfo.getConstructor(MappedRangeNumberArray.class);

	public NumberArray array;

	public MappedRangeNumberArray(NumberArray array) {
		this.array = array;
	}

	@Override
	public void reallocate(int requiredLength) {
		this.valid = true;
		if (this.array.length() < requiredLength) {
			requiredLength = Math.max(requiredLength, this.array.length() << 1);
			this.array.close();
			this.array = switch (this.array.type) {
				case NumberArray.   BYTE_TYPE -> NumberArray.allocateBytesHeap   (requiredLength);
				case NumberArray.  SHORT_TYPE -> NumberArray.allocateShortsHeap  (requiredLength);
				case NumberArray.    INT_TYPE -> NumberArray.allocateIntsHeap    (requiredLength);
				case NumberArray.   LONG_TYPE -> NumberArray.allocateLongsHeap   (requiredLength);
				case NumberArray.  FLOAT_TYPE -> NumberArray.allocateFloatsHeap  (requiredLength);
				case NumberArray. DOUBLE_TYPE -> NumberArray.allocateDoublesHeap (requiredLength);
				case NumberArray.BOOLEAN_TYPE -> NumberArray.allocateBooleansHeap(requiredLength);
				default -> throw new IllegalStateException("Invalid type: " + this.array.type);
			};
		}
	}
}