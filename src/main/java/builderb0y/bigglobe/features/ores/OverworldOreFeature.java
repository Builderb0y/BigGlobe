package builderb0y.bigglobe.features.ores;

import net.minecraft.block.BlockState;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer.TwoBlockReplacer;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public class OverworldOreFeature extends OreFeature<OverworldOreFeature.Config> {

	public OverworldOreFeature() {
		super(Config.class);
	}

	public static class Config extends OreFeature.Config {

		public final @VerifyNormal BlockState stone_state, deepslate_state;

		public Config(
			BlockState stone_state,
			BlockState deepslate_state,
			ColumnYToDoubleScript.Holder chance,
			RandomSource radius
		) {
			super(chance, radius);
			this.stone_state = stone_state;
			this.deepslate_state = deepslate_state;
		}

		@Override
		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return new TwoBlockReplacer(context, BlockStates.STONE, this.stone_state, BlockStates.DEEPSLATE, this.deepslate_state);
		}
	}
}