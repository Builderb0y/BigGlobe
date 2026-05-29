package builderb0y.bigglobe.features;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

public interface RockReplacerFeature<T_Config extends FeatureConfiguration> {

	public abstract void replaceRocks(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		ChunkAccess chunk,
		int minSection,
		int maxSection,
		T_Config config
	);

	public static record ConfiguredRockReplacerFeature<T_Config extends FeatureConfiguration>(RockReplacerFeature<T_Config> feature, T_Config config) {

		@SuppressWarnings("unchecked")
		public ConfiguredRockReplacerFeature(ConfiguredFeature<?, ?> configuredFeature) {
			this(
				(RockReplacerFeature<T_Config>)(configuredFeature.feature()),
				(T_Config)(configuredFeature.config())
			);
		}

		public void replaceRocks(
			BigGlobeScriptedChunkGenerator generator,
			WorldWrapper worldWrapper,
			ChunkAccess chunk,
			int minSection,
			int maxSection
		) {
			this.feature.replaceRocks(
				generator,
				worldWrapper,
				chunk,
				minSection,
				maxSection,
				this.config
			);
		}
	}
}