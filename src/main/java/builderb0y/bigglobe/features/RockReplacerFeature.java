package builderb0y.bigglobe.features;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FeatureConfig;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;

public interface RockReplacerFeature<T_Config extends FeatureConfig> {

	public abstract void replaceRocks(
		BigGlobeScriptedChunkGenerator generator,
		ScriptedColumnLookup columns,
		Chunk chunk,
		int minSection,
		int maxSection,
		T_Config config
	);

	public static record ConfiguredRockReplacerFeature<T_Config extends FeatureConfig>(RockReplacerFeature<T_Config> feature, T_Config config) {

		@SuppressWarnings("unchecked")
		public ConfiguredRockReplacerFeature(ConfiguredFeature<?, ?> configuredFeature) {
			this(
				(RockReplacerFeature<T_Config>)(configuredFeature.feature()),
				(T_Config)(configuredFeature.config())
			);
		}

		public void replaceRocks(
			BigGlobeScriptedChunkGenerator generator,
			ScriptedColumnLookup columns,
			Chunk chunk,
			int minSection,
			int maxSection
		) {
			this.feature.replaceRocks(
				generator,
				columns,
				chunk,
				minSection,
				maxSection,
				this.config
			);
		}
	}
}