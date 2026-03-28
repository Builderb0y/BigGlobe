package builderb0y.scripting.parsing;

import org.jetbrains.annotations.Nullable;

public class ScriptParsingException extends Exception {

	public ScriptParsingException(String message, ExpressionReader input) {
		super(appendContext(message, input));
	}

	public ScriptParsingException(String message, Throwable cause, ExpressionReader input) {
		super(appendContext(message, input), cause);
	}

	public ScriptParsingException(Throwable cause, ExpressionReader input) {
		super(appendContext(cause.getMessage(), input), cause);
	}

	public ScriptParsingException(ScriptParsingException cause) {
		super(cause.getMessage(), cause);
	}

	public static @Nullable String appendContext(@Nullable String message, @Nullable ExpressionReader input) {
		if (message != null) {
			if (input != null) {
				return message + " at line " + input.line + ", column " + input.column + ":\n" + input.getSourceForError() + " <--- HERE";
			}
			else {
				return message;
			}
		}
		else {
			if (input != null) {
				return "Unknown error at line " + input.line + ", column " + input.column + ":\n" + input.getSourceForError() + " <--- HERE";
			}
			else {
				return null;
			}
		}
	}
}