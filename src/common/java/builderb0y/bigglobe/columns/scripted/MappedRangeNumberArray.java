package builderb0y.bigglobe.columns.scripted;

import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

public class MappedRangeNumberArray extends MappedRangeArray {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo
			array;
		public MethodInfo
			trySetZ,
			trySetB,
			trySetS,
			trySetI,
			trySetL,
			trySetF,
			trySetD,
			cachedPrefix;
	}

	public static final MethodInfo CONSTRUCTOR = MethodInfo.getConstructor(MappedRangeNumberArray.class);

	public NumberArray array;

	public MappedRangeNumberArray(NumberArray array) {
		this.array = array;
	}

	public void trySetZ(int y, boolean value) {
		NumberArray array;
		if ((y -= this.minCached) >= 0 && y < (array = this.array).length()) {
			array.implSetZ(y, value);
		}
	}

	public void trySetB(int y, byte value) {
		NumberArray array;
		if ((y -= this.minCached) >= 0 && y < (array = this.array).length()) {
			array.implSetB(y, value);
		}
	}

	public void trySetS(int y, short value) {
		NumberArray array;
		if ((y -= this.minCached) >= 0 && y < (array = this.array).length()) {
			array.implSetS(y, value);
		}
	}

	public void trySetI(int y, int value) {
		NumberArray array;
		if ((y -= this.minCached) >= 0 && y < (array = this.array).length()) {
			array.implSetI(y, value);
		}
	}

	public void trySetL(int y, long value) {
		NumberArray array;
		if ((y -= this.minCached) >= 0 && y < (array = this.array).length()) {
			array.implSetL(y, value);
		}
	}

	public void trySetF(int y, float value) {
		NumberArray array;
		if ((y -= this.minCached) >= 0 && y < (array = this.array).length()) {
			array.implSetF(y, value);
		}
	}

	public void trySetD(int y, double value) {
		NumberArray array;
		if ((y -= this.minCached) >= 0 && y < (array = this.array).length()) {
			array.implSetD(y, value);
		}
	}

	@Override
	public boolean reallocate(int requiredLength) {
		this.valid = true;
		if (this.array.length() < requiredLength) {
			requiredLength = Math.max(requiredLength, this.array.length() * 3 / 2);
			this.array.close();
			this.array = switch (this.array.type) {
				case NumberArray.BYTE_TYPE -> NumberArray.allocateBytesHeap(requiredLength);
				case NumberArray.SHORT_TYPE -> NumberArray.allocateShortsHeap(requiredLength);
				case NumberArray.INT_TYPE -> NumberArray.allocateIntsHeap(requiredLength);
				case NumberArray.LONG_TYPE -> NumberArray.allocateLongsHeap(requiredLength);
				case NumberArray.FLOAT_TYPE -> NumberArray.allocateFloatsHeap(requiredLength);
				case NumberArray.DOUBLE_TYPE -> NumberArray.allocateDoublesHeap(requiredLength);
				case NumberArray.BOOLEAN_TYPE -> NumberArray.allocateBooleansHeap(requiredLength);
				default -> throw new IllegalStateException("Invalid type: " + this.array.type);
			};
		}
		return requiredLength > 0;
	}

	public NumberArray cachedPrefix() {
		return this.array.prefix(this.cachedRange());
	}
}