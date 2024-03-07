package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.bigglobe.BigGlobeMod;

public class DummyFeature<T_Config extends DummyFeature.DummyConfig> extends Feature<T_Config> {

	public DummyFeature(Codec<T_Config> configCodec) {
		super(configCodec);
	}

	@Override
	public boolean generate(FeatureContext<T_Config> context) {
		if (!context.getConfig().warned) {
			context.getConfig().warned = true;
			BigGlobeMod.LOGGER.warn(this.getClass().getSimpleName() + " is a *special* feature which requires cooperation from my own chunk generator. It cannot be placed in the world directly. If you are making a data pack, do NOT add this feature to any biomes.");
		}
		return false;
	}

	public static class DummyConfig implements FeatureConfig {

		public transient boolean warned;
	}
}