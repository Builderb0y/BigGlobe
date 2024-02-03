package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.ArrayList;
import java.util.List;

public class DecisionTreeException extends RuntimeException {

	public List<String> details = new ArrayList<>(8);

	public DecisionTreeException() {
	}

	public DecisionTreeException(String message) {
		super(message);
	}

	public DecisionTreeException(String message, Throwable cause) {
		super(message, cause);
	}

	public DecisionTreeException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getLocalizedMessage() {
		StringBuilder builder = new StringBuilder(super.getLocalizedMessage());
		for (String detail : this.details) {
			builder.append("\n\t").append(detail);
		}
		return builder.toString();
	}
}