package builderb0y.bigglobe.rendering2.lods;

public class OutOfVramException extends RuntimeException {

	public OutOfVramException() {}

	public OutOfVramException(String message) {
		super(message);
	}

	public OutOfVramException(Throwable cause) {
		super(cause);
	}

	public OutOfVramException(String message, Throwable cause) {
		super(message, cause);
	}
}