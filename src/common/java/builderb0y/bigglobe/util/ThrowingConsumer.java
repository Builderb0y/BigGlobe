package builderb0y.bigglobe.util;

@FunctionalInterface
public interface ThrowingConsumer<T, X extends Throwable> {

	public abstract void accept(T object) throws X;
}