package builderb0y.scripting.parsing;

import org.jetbrains.annotations.Nullable;

public interface Script {

	/** returns the source code that this Script was compiled from. */
	public abstract String getSource();

	/**
	returns the debug_name specified in the JSON object
	which contains this Script's source, if present.
	if not present, returns null.
	*/
	public abstract @Nullable String getDebugName();
}