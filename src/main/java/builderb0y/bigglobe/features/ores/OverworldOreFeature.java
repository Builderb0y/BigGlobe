package builderb0y.bigglobe.features.ores;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
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
			PaletteStorage storage  = context.storage();
			int stoneID             = context.id(BlockStates.STONE);
			int deepslateID         = context.id(BlockStates.DEEPSLATE);
			int stoneOreID          = context.id(this.stone_state);
			int deepslateOreID      = context.id(this.deepslate_state);
			if (storage != (storage = context.storage())) { //resize
				stoneID             = context.id(BlockStates.STONE);
				deepslateID         = context.id(BlockStates.DEEPSLATE);
				stoneOreID          = context.id(this.stone_state);
				deepslateOreID      = context.id(this.deepslate_state);
				assert storage     == context.storage();
			}
			return new TwoBlockReplacer(stoneID, deepslateID, stoneOreID, deepslateOreID);
		}

		public static class TwoBlockReplacer implements PaletteIdReplacer {

			public int from1, from2, to1, to2;

			public TwoBlockReplacer(int from1, int from2, int to1, int to2) {
				this.from1 = from1;
				this.from2 = from2;
				this.to1 = to1;
				this.to2 = to2;
			}

			@Override
			public int getReplacement(int id) {
				if (id == this.from1) return this.to1;
				if (id == this.from2) {
					this.swap();
					return this.to1; //just swapped, so to2 became to1.
				}
				return id;
			}

			public void swap() {
				int tmp = this.from1;
				this.from1 = this.from2;
				this.from2 = tmp;
				tmp = this.to1;
				this.to1 = this.to2;
				this.to2 = tmp;
			}
		}
	}
}