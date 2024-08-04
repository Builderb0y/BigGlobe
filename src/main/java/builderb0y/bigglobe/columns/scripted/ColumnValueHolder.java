package builderb0y.bigglobe.columns.scripted;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Stream;

import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.ScriptClassLoader;

/**
an interface to facilitate inter-op between runtime-generated classes
and regular classes. also useful for debuggers.
runtime-generated subclasses of {@link ScriptedColumn} and {@link VoronoiDataBase}
will implement the methods on this interface,
and they are implemented as a switch statement which uses the
"name" parameter to delegate to the correct method to call.
*/
public interface ColumnValueHolder {

	/**
	returns true if the column value associated with (name) has already been computed.

	@throws IllegalArgumentException if (name) does not represent
	a column value known to this holder, or if the column value
	associated with (name) does not have an associated field and flag.
	*/
	public abstract boolean isColumnValuePresent(String name);

	/**
	returns the column value associated with (name), wrapped in an object if necessary.

	@throws IllegalArgumentException if (name) does not represent a column value known to this holder.

	@throws Throwable if the act of computing this column value threw an exception.
	the exception is not wrapped.
	*/
	public abstract Object getColumnValue(String name, int y) throws Throwable;

	/**
	sets the column value associated with (name) to (value) at (y) if the column value is 3D.
	if the column value is 2D, then (y) is ignored.

	@throws IllegalArgumentException if (name) does not represent
	a column value known to this holder, or if the column value
	associated with (name) does not have an associated field and flag.
	*/
	public abstract void setColumnValue(String name, int y, Object value);

	/**
	ensures that the column value represented by (name) {@link #isColumnValuePresent(String) is present}.
	if the column value represented by (name) is not present, it is computed, but not returned.

	@throws Throwable if the act of computing the column value threw an exception.
	the exception is not wrapped.
	*/
	public abstract void preComputeColumnValue(String name) throws Throwable;

	/**
	returns information about all the column values present on this holder.
	the returned list will be immutable.
	the returned list will be sorted by natural order of the name of the column value.
	*/
	public abstract List<ColumnValueInfo> getColumnValues();

	public static record ColumnValueInfo(String name, Class<?> type, boolean dependsOnY, Mutability mutability) {

		public static final TypeInfo TYPE = TypeInfo.of(ColumnValueInfo.class);

		public static enum Mutability {
			COMPUTED(false, false),
			CACHED(true, true),
			VORONOI(true, false);

			public final boolean hasField, isSettable;

			Mutability(boolean hasField, boolean isSettable) {
				this.hasField = hasField;
				this.isSettable = isSettable;
			}
		}
	}

	public static record UnresolvedColumnValueInfo(String name, TypeInfo type, boolean dependsOnY, ColumnValueInfo.Mutability mutability) {

		public static final MethodInfo RESOLVE = MethodInfo.inCaller("resolveAll");

		public ColumnValueInfo resolve(ClassLoader loader) {
			return new ColumnValueInfo(this.name, this.type.toClass(loader), this.dependsOnY, this.mutability);
		}

		public static List<ColumnValueInfo> resolveAll(MethodHandles.Lookup caller, String name, Class<?> type, UnresolvedColumnValueInfo[] args) {
			ClassLoader loader = caller.lookupClass().getClassLoader();
			return Stream.of(args).map((UnresolvedColumnValueInfo unresolved) -> unresolved.resolve(loader)).toList();
		}
	}
}