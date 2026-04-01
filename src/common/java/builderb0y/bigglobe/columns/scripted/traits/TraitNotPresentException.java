package builderb0y.bigglobe.columns.scripted.traits;

public class TraitNotPresentException extends RuntimeException {

	public TraitNotPresentException() {}

	public TraitNotPresentException(String message) {
		super(message);
	}

	public TraitNotPresentException(Throwable cause) {
		super(cause);
	}

	public TraitNotPresentException(String message, Throwable cause) {
		super(message, cause);
	}
}