package builderb0y.bigglobe.compat.voxy;

public interface QueueingStorageBackend {

	public abstract AbstractVoxyWorldGenerator getGenerator();

	public abstract void setGenerator(AbstractVoxyWorldGenerator generator);
}