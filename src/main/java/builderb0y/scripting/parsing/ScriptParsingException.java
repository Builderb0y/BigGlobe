package builderb0y.scripting.parsing;

import org.jetbrains.annotations.Nullable;

public class ScriptParsingException extends Exception {

	public @Nullable String at;

	public ScriptParsingException(String message, ExpressionReader input) {
		super(message);
		this.at = appendContext(input);
	}

	public ScriptParsingException(String message, Throwable cause, ExpressionReader input) {
		super(message, cause);
		this.at = appendContext(input);
	}

	public ScriptParsingException(Throwable cause, ExpressionReader input) {
		super(cause.getMessage(), cause);
		this.at = appendContext(input);
	}

	@Override
	public String getLocalizedMessage() {
		return this.at == null ? this.getMessage() : this.getMessage() + ' ' + this.at;
	}

	public static @Nullable String appendContext(ExpressionReader input) {
		return input == null ? null : "at line " + input.line + ", column " + input.column + ":\n" + input.getSourceForError() + " <--- HERE";
	}
}