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

	public ScriptParsingException(ScriptParsingException cause) {
		super(cause.getLocalizedMessage(), cause);
		this.at = null;
		//for some dumb reason, the following code does not work:
		//	super(cause.getMessage(), cause);
		//	this.at = cause.at;
		//the effect of that code is that when the stack trace is printed,
		//it does not contain our at information.
		//I don't know why this is the case.
	}

	@Override
	public String getLocalizedMessage() {
		return this.at == null ? this.getMessage() : this.getMessage() + ' ' + this.at;
	}

	public static @Nullable String appendContext(ExpressionReader input) {
		return input == null ? null : "at line " + input.line + ", column " + input.column + ":\n" + input.getSourceForError() + " <--- HERE";
	}
}