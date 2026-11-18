package builderb0y.bigglobe.features;

public interface SizedDelayedFeature<C extends FeatureAdapter.Config> {

	#if MC_VERSION >= MC_26_3_0

		public abstract C getSizedDelayedConfig();

	#endif

	public abstract int getMaxRadiusInBlocks(C config);
}