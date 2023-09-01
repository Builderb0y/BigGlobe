package builderb0y.scripting.util;

import java.io.PrintStream;
import java.util.Objects;

public interface PrintSink {

	public abstract void println(int value);

	public abstract void println(long value);

	public abstract void println(float value);

	public abstract void println(double value);

	public abstract void println(char value);

	public abstract void println(boolean value);

	public abstract void println(String value);

	public abstract void println(Object value);

	public static PrintSink forPrintStream(PrintStream stream) {
		Objects.requireNonNull(stream, "stream");
		return new PrintSink() {
			@Override public void println(int     value) { stream.println(value); }
			@Override public void println(long    value) { stream.println(value); }
			@Override public void println(float   value) { stream.println(value); }
			@Override public void println(double  value) { stream.println(value); }
			@Override public void println(char    value) { stream.println(value); }
			@Override public void println(boolean value) { stream.println(value); }
			@Override public void println(String  value) { stream.println(value); }
			@Override public void println(Object  value) { stream.println(value); }
		};
	}
}