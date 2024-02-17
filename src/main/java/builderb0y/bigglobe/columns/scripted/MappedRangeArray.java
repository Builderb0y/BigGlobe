package builderb0y.bigglobe.columns.scripted;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;

public abstract class MappedRangeArray {

	public static final FieldInfo
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
		REALLOCATE_BOTH = MethodInfo.inCaller("reallocateBoth");

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