package builderb0y.scripting.bytecode.tree;

public class InvalidOperandException extends IllegalArgumentException {

	public InvalidOperandException() {}

	public InvalidOperandException(String s) {
		super(s);
	}

	public InvalidOperandException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidOperandException(Throwable cause) {
		super(cause);
	}
}