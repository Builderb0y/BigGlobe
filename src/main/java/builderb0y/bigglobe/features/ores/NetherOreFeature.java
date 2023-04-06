package builderb0y.bigglobe.features.ores;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer.OneBlockReplacer;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public class NetherOreFeature extends OreFeature<NetherOreFeature.Config> {

	public NetherOreFeature() {
		super(Config.class);
	}

	public static class Config extends OreFeature.Config {

		public final @VerifyNormal BlockState place, replace;
		public final TagKey<Biome> biomes;

		public Config(
			ColumnYToDoubleScript.Holder chance,
			RandomSource radius,
			BlockState place,
			BlockState replace,
			TagKey<Biome> biomes
		) {
			super(chance, radius);
			this.place   = place;
			this.replace = replace;
			this.biomes  = biomes;
		}

		@Override
		public boolean canSpawnAt(WorldColumn column, int y) {
			return column.getBiome(y).isIn(this.biomes);
		}

		@Override
		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return new OneBlockReplacer(context, this.replace, this.place);
		}
	}
}