package builderb0y.bigglobe.util;

@FunctionalInterface
public interface ThrowingSupplier<T, X extends Throwable> {

	public abstract T get() throws X;
}