package builderb0y.bigglobe.columns.scripted2;

import builderb0y.bigglobe.classes.compile.DetailedException;

public class ColumnValueException extends DetailedException {

	public ColumnValueException() {}

	public ColumnValueException(String message) {
		super(message);
	}

	public ColumnValueException(Throwable cause) {
		super(cause);
	}

	public ColumnValueException(String message, Throwable cause) {
		super(message, cause);
	}
}