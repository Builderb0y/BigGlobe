package builderb0y.bigglobe.columns.scripted2;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

public abstract class MappedRangeArray {

	public static final Info INFO = new Info();

	public static class Info extends InfoHolder {

		public FieldInfo
			valid,
			minCached,
			maxCached,
			minAccessible,
			maxAccessible;
		public MethodInfo
			invalidate,
			reallocateNone,
			reallocateMin,
			reallocateMax,
			reallocateBoth,
			cachedRange,
			accessibleRange;
	}

	public int minCached, maxCached, minAccessible, maxAccessible;
	public boolean valid = true;

	public void invalidate() {
		this.valid = false;
	}

	public boolean reallocateNone(ScriptedColumn column) {
		return this.reallocate(
			(this.maxCached = column.maxY())
			-
			(this.minCached = column.minY())
		);
	}

	public boolean reallocateMin(ScriptedColumn column, int minAccessible) {
		return this.reallocate(
			(this.maxCached = column.maxY())
			-
			(this.minCached = Math.max(column.minY(), this.minAccessible = minAccessible))
		);
	}

	public boolean reallocateMax(ScriptedColumn column, int maxAccessible) {
		return this.reallocate(
			(this.maxCached = Math.min(column.maxY(), this.maxAccessible = maxAccessible))
			-
			(this.minCached = column.minY())
		);
	}

	public boolean reallocateBoth(ScriptedColumn column, int minAccessible, int maxAccessible) {
		return this.reallocate(
			(this.maxCached = Math.min(column.maxY(), this.maxAccessible = maxAccessible))
			-
			(this.minCached = Math.max(column.minY(), this.minAccessible = minAccessible))
		);
	}

	/**
	@return requiredLength > 0. this implies that the runtime-generated
	code should proceed with filling the array with values.
	*/
	public abstract boolean reallocate(int requiredLength);

	public int cachedRange() {
		return this.maxCached - this.minCached;
	}

	public int accessibleRange() {
		return this.maxAccessible - this.minAccessible;
	}
}