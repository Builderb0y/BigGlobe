package builderb0y.bigglobe.classes.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DetailedException extends Exception {

	public Class<? extends Throwable>
		clazz = this.getClass();
	public List<String>
		messageParts = new ArrayList<>(),
		lines = new ArrayList<>();

	public DetailedException() {}

	public DetailedException(String message) {
		super(message);
		this.messageParts.add(message);
	}

	public DetailedException(Throwable cause) {
		super(cause);
	}

	public DetailedException(String message, Throwable cause) {
		super(message, cause);
		this.messageParts.add(message);
	}

	public DetailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public void prependMessage(String message) {
		this.messageParts.addFirst(message);
	}

	public void appendMessage(String message) {
		this.messageParts.addLast(message);
	}

	public void prependLine(String line) {
		this.lines.addFirst(line);
	}

	public void appendLine(String line) {
		this.lines.addLast(line);
	}

	public static DetailedException adapt(Throwable throwable) {
		if (throwable instanceof DetailedException detailed) {
			return detailed;
		}
		DetailedException detailed = new DetailedException(null, null, true, true);
		detailed.clazz = throwable.getClass();
		String message = throwable.getLocalizedMessage();
		if (message != null) detailed.messageParts.add(message);
		detailed.setStackTrace(throwable.getStackTrace());
		Throwable cause = throwable.getCause();
		if (cause != null) detailed.initCause(cause);
		for (Throwable suppress : throwable.getSuppressed()) {
			detailed.addSuppressed(suppress);
		}
		return detailed;
	}

	public static DetailedException adapt(Throwable throwable, Consumer<DetailedException> action) {
		DetailedException detailed = adapt(throwable);
		action.accept(detailed);
		return detailed;
	}

	@Override
	public String getMessage() {
		StringBuilder builder = new StringBuilder();
		for (String message : this.messageParts) {
			if (!builder.isEmpty()) builder.append("; ");
			builder.append(message);
		}
		for (String line : this.lines) {
			builder.append('\n').append('\t').append(line);
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		Class<?> clazz = this.clazz;
		String message = this.getLocalizedMessage();
		return message.isEmpty() ? clazz.getName() : (clazz.getName() + ": " + message);
	}
}