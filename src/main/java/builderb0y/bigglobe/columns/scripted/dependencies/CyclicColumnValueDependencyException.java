package builderb0y.bigglobe.columns.scripted.dependencies;

public class CyclicColumnValueDependencyException extends RuntimeException {

	public CyclicColumnValueDependencyException() {}

	public CyclicColumnValueDependencyException(String message) {
		super(message);
	}

	public CyclicColumnValueDependencyException(String message, Throwable cause) {
		super(message, cause);
	}

	public CyclicColumnValueDependencyException(Throwable cause) {
		super(cause);
	}
}