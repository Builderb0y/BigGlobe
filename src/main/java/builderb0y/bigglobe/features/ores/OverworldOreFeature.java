package builderb0y.bigglobe.features.ores;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class OverworldOreFeature extends OreFeature<OverworldOreFeature.Config> {

	public OverworldOreFeature() {
		super(Config.class);
	}

	public static class Config extends OreFeature.Config {

		public final @VerifyNormal @VerifyNullable BlockState stone_state, deepslate_state;

		public Config(
			ColumnYToDoubleScript.Holder chance,
			RandomSource radius,
			BlockState2ObjectMap<BlockState> blocks,
			@VerifyNormal @VerifyNullable BlockState stone_state,
			@VerifyNormal @VerifyNullable BlockState deepslate_state
		) {
			super(chance, radius, blocks);
			if (stone_state != null) blocks.runtimeStates.put(BlockStates.STONE, stone_state);
			if (deepslate_state != null) blocks.runtimeStates.put(BlockStates.DEEPSLATE, deepslate_state);
			this.stone_state = stone_state;
			this.deepslate_state = deepslate_state;
		}
	}
}