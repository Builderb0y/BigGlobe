package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import builderb0y.bigglobe.BigGlobeMod;

public class DummyFeature<T_Config extends DummyFeature.DummyConfig> extends Feature<T_Config> {

	public DummyFeature(Codec<T_Config> configCodec) {
		super(configCodec);
	}

	@Override
	public boolean place(FeaturePlaceContext<T_Config> context) {
		if (!context.config().warned) {
			context.config().warned = true;
			BigGlobeMod.LOGGER.warn(this.getClass().getSimpleName() + " is a *special* feature which cannot be placed in the world directly. If you are making a data pack, do NOT add this feature to any biomes.");
		}
		return false;
	}

	public static class DummyConfig implements FeatureConfiguration {

		public transient boolean warned;
	}
}