package builderb0y.bigglobe.columns.scripted;

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
			reallocateBoth;
	}

	public int minCached, maxCached, minAccessible, maxAccessible;
	public boolean valid = true;

	public void invalidate() {
		this.valid = false;
	}

	public void reallocateNone(ScriptedColumn column) {
		this.reallocate(
			(this.maxCached = column.maxY())
			-
			(this.minCached = column.minY())
		);
	}

	public void reallocateMin(ScriptedColumn column, int minAccessible) {
		this.reallocate(
			(this.maxCached = column.maxY())
			-
			(this.minCached = Math.max(column.minY(), this.minAccessible = minAccessible))
		);
	}

	public void reallocateMax(ScriptedColumn column, int maxAccessible) {
		this.reallocate(
			(this.maxCached = Math.min(column.maxY(), this.maxAccessible = maxAccessible))
			-
			(this.minCached = column.minY())
		);
	}

	public void reallocateBoth(ScriptedColumn column, int minAccessible, int maxAccessible) {
		this.reallocate(
			(this.maxCached = Math.min(column.maxY(), this.maxAccessible = maxAccessible))
			-
			(this.minCached = Math.max(column.minY(), this.minAccessible = minAccessible))
		);
	}

	public abstract void reallocate(int requiredLength);
}