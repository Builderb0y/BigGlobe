package builderb0y.bigglobe.columns.scripted;

import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MappedRangeNumberArray extends MappedRangeArray {

	public static final TypeInfo
		TYPE = type(MappedRangeNumberArray.class);
	public static final FieldInfo
		ARRAY           =  FieldInfo.inCaller("array");
	public static final MethodInfo
		CONSTRUCT       = MethodInfo.getConstructor(MappedRangeNumberArray.class),

		GET_B = MethodInfo.getMethod(NumberArray.class, "getB"),
		GET_S = MethodInfo.getMethod(NumberArray.class, "getS"),
		GET_I = MethodInfo.getMethod(NumberArray.class, "getI"),
		GET_L = MethodInfo.getMethod(NumberArray.class, "getL"),
		GET_F = MethodInfo.getMethod(NumberArray.class, "getF"),
		GET_D = MethodInfo.getMethod(NumberArray.class, "getD"),
		GET_Z = MethodInfo.getMethod(NumberArray.class, "getZ"),

		SET_B = MethodInfo.getMethod(NumberArray.class, "setB"),
		SET_S = MethodInfo.getMethod(NumberArray.class, "setS"),
		SET_I = MethodInfo.getMethod(NumberArray.class, "setI"),
		SET_L = MethodInfo.getMethod(NumberArray.class, "setL"),
		SET_F = MethodInfo.getMethod(NumberArray.class, "setF"),
		SET_D = MethodInfo.getMethod(NumberArray.class, "setD"),
		SET_Z = MethodInfo.getMethod(NumberArray.class, "setZ");

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
			this.array = switch (this.array.getPrecision()) {
				case BYTE    -> NumberArray.allocateBytesHeap   (requiredLength);
				case SHORT   -> NumberArray.allocateShortsHeap  (requiredLength);
				case INT     -> NumberArray.allocateIntsHeap    (requiredLength);
				case LONG    -> NumberArray.allocateLongsHeap   (requiredLength);
				case FLOAT   -> NumberArray.allocateFloatsHeap  (requiredLength);
				case DOUBLE  -> NumberArray.allocateDoublesHeap (requiredLength);
				case BOOLEAN -> NumberArray.allocateBooleansHeap(requiredLength);
			};
		}
	}
}