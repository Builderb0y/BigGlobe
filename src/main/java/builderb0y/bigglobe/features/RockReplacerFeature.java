package builderb0y.bigglobe.features;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FeatureConfig;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

public interface RockReplacerFeature<T_Config extends FeatureConfig> {

	public abstract void replaceRocks(BigGlobeScriptedChunkGenerator generator, Chunk chunk, T_Config config);

	public static record ConfiguredRockReplacerFeature<T_Config extends FeatureConfig>(RockReplacerFeature<T_Config> feature, T_Config config) {

		@SuppressWarnings("unchecked")
		public ConfiguredRockReplacerFeature(ConfiguredFeature<?, ?> configuredFeature) {
			this((RockReplacerFeature<T_Config>)(configuredFeature.feature()), (T_Config)(configuredFeature.config()));
		}

		public void replaceRocks(BigGlobeScriptedChunkGenerator generator, Chunk chunk) {
			this.feature.replaceRocks(generator, chunk, this.config);
		}
	}
}