package builderb0y.bigglobe.util;

@FunctionalInterface
public interface ThrowingFunction<In, Out, X extends Throwable> {

	public abstract Out apply(In argument) throws X;
}