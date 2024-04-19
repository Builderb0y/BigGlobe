package builderb0y.bigglobe.columns.scripted;

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
}