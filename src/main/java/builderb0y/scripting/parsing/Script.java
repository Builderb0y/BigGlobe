package builderb0y.scripting.parsing;

/**
subclasses of {@link ExpressionParser} will produce instances of this interface.
the method {@link #getSource()} can be used to return
the source code that this Script was compiled from.
*/
public interface Script {

	/** returns the source code that this Script was compiled from. */
	public abstract String getSource();
}