package builderb0y.bigglobe.columns.scripted.dependencies;

public class CyclicDependencyException extends RuntimeException {

	public CyclicDependencyException() {}

	public CyclicDependencyException(String message) {
		super(message);
	}

	public CyclicDependencyException(String message, Throwable cause) {
		super(message, cause);
	}

	public CyclicDependencyException(Throwable cause) {
		super(cause);
	}
}