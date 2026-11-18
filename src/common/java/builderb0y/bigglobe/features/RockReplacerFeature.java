package builderb0y.bigglobe.features;

import net.minecraft.world.level.chunk.ChunkAccess;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
	import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
#endif

public interface RockReplacerFeature<T_Config extends FeatureAdapter.Config> {

	#if MC_VERSION >= MC_26_3_0

		public abstract T_Config config();

	#endif

	public abstract void replaceRocks(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		ChunkAccess chunk,
		int minSection,
		int maxSection,
		T_Config config
	);

	public static record ConfiguredRockReplacerFeature<T_Config extends FeatureAdapter.Config>(RockReplacerFeature<T_Config> feature, T_Config config) {

		#if MC_VERSION >= MC_26_3_0

			public ConfiguredRockReplacerFeature(RockReplacerFeature<T_Config> feature) {
				this(feature, feature.config());
			}

		#else

			@SuppressWarnings("unchecked")
			public ConfiguredRockReplacerFeature(ConfiguredFeature<?, ?> configuredFeature) {
				this(
					(RockReplacerFeature<T_Config>)(configuredFeature.feature()),
					(T_Config)(configuredFeature.config())
				);
			}

		#endif

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