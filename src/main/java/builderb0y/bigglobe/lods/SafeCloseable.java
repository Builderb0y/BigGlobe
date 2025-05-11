package builderb0y.bigglobe.lods;

public interface SafeCloseable extends AutoCloseable {

	public static final SafeCloseable NOOP = () -> {};

	@Override
	public abstract void close();
}