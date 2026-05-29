package builderb0y.scripting.parsing;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.classes.compile.DetailedException;

public class ScriptParsingException extends DetailedException {

	public ScriptParsingException(String message, ExpressionReader input) {
		super(message);
		this.addContext(input);
	}

	public ScriptParsingException(String message, Throwable cause, ExpressionReader input) {
		super(message, cause);
		this.addContext(input);
	}

	public ScriptParsingException(Throwable cause, ExpressionReader input) {
		super(cause);
		this.addContext(input);
	}

	public void addContext(ExpressionReader input) {
		if (input != null) {
			this.appendLine("at line " + input.line + ", column " + input.column);
			input.getSourceForError().lines().forEachOrdered(this::appendLine);
			int index = this.lines.size() - 1;
			this.lines.set(index, this.lines.get(index) + " <--- HERE");
		}
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