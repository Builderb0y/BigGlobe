package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import builderb0y.bigglobe.BigGlobeMod;

public class DummyFeature<T_Config extends DummyFeature.DummyConfig> extends FeatureAdapter<T_Config> {

	#if MC_VERSION >= MC_26_3_0

		public DummyFeature(T_Config config) {
			super(config);
		}

	#else

		public DummyFeature(Codec<T_Config> configCodec) {
			super(configCodec);
		}

	#endif

	@Override
	public boolean place(Context<T_Config> context) {
		if (!context.config().warned) {
			context.config().warned = true;
			BigGlobeMod.LOGGER.warn(this.getClass().getSimpleName() + " is a *special* feature which cannot be placed in the world directly. If you are making a data pack, do NOT add this feature to any biomes.");
		}
		return false;
	}

	public static class DummyConfig implements FeatureAdapter.Config {

		public transient boolean warned;
	}
}