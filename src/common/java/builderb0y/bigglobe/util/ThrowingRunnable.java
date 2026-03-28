package builderb0y.bigglobe.util;

@FunctionalInterface
public interface ThrowingRunnable<X extends Throwable> {

	public abstract void run() throws X;
}