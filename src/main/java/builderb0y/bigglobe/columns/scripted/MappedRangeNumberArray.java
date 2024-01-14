package builderb0y.bigglobe.columns.scripted;

import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;

public class MappedRangeNumberArray {

	public static final FieldInfo
		ARRAY           =  FieldInfo.inCaller("array"),
		VALID           =  FieldInfo.inCaller("valid"),
		MIN_CACHED      =  FieldInfo.inCaller("minCached"),
		MAX_CACHED      =  FieldInfo.inCaller("maxCached"),
		MIN_ACCESSIBLE  =  FieldInfo.inCaller("minAccessible"),
		MAX_ACCESSIBLE  =  FieldInfo.inCaller("maxAccessible");
	public static final MethodInfo
		INVALIDATE      = MethodInfo.inCaller("invalidate"),
		REALLOCATE_NONE = MethodInfo.inCaller("reallocateNone"),
		REALLOCATE_MIN  = MethodInfo.inCaller("reallocateMin"),
		REALLOCATE_MAX  = MethodInfo.inCaller("reallocateMax"),
		REALLOCATE_BOTH = MethodInfo.inCaller("reallocateBoth"),
		GET_B = MethodInfo.getMethod(NumberArray.class, "getB"),
		GET_S = MethodInfo.getMethod(NumberArray.class, "getS"),
		GET_I = MethodInfo.getMethod(NumberArray.class, "getI"),
		GET_L = MethodInfo.getMethod(NumberArray.class, "getL"),
		GET_F = MethodInfo.getMethod(NumberArray.class, "getF"),
		GET_D = MethodInfo.getMethod(NumberArray.class, "getD");

	public NumberArray array;
	public int minCached, maxCached, minAccessible, maxAccessible;
	public boolean valid;

	public MappedRangeNumberArray(NumberArray array) {
		this.array = array;
		this.valid = true;
	}

	public void invalidate() {
		this.valid = false;
	}

	public void reallocateNone(ScriptedColumn column) {
		this.reallocate(
			(this.maxCached = column.maxY)
			-
			(this.minCached = column.minY)
		);
	}

	public void reallocateMin(ScriptedColumn column, int minAccessible) {
		this.reallocate(
			(this.maxCached = column.maxY)
			-
			(this.minCached = Math.max(column.minY, this.minAccessible = minAccessible))
		);
	}

	public void reallocateMax(ScriptedColumn column, int maxAccessible) {
		this.reallocate(
			(this.maxCached = Math.min(column.maxY, this.maxAccessible = maxAccessible))
			-
			(this.minCached = column.minY)
		);
	}

	public void reallocateBoth(ScriptedColumn column, int minAccessible, int maxAccessible) {
		this.reallocate(
			(this.maxCached = Math.min(column.maxY, this.maxAccessible = maxAccessible))
			-
			(this.minCached = Math.max(column.minY, this.minAccessible = minAccessible))
		);
	}

	public void reallocate(int requiredLength) {
		this.valid = true;
		if (this.array.length() < requiredLength) {
			requiredLength = Math.max(requiredLength, this.array.length() << 1);
			this.array.close();
			this.array = switch (this.array.getPrecision()) {
				case BYTE   -> NumberArray.allocateBytesHeap  (requiredLength);
				case SHORT  -> NumberArray.allocateShortsHeap (requiredLength);
				case INT    -> NumberArray.allocateIntsHeap   (requiredLength);
				case LONG   -> NumberArray.allocateLongsHeap  (requiredLength);
				case FLOAT  -> NumberArray.allocateFloatsHeap (requiredLength);
				case DOUBLE -> NumberArray.allocateDoublesHeap(requiredLength);
			};
		}
	}
}