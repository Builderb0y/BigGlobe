package builderb0y.bigglobe.features.ores;

import net.minecraft.block.BlockState;
import net.minecraft.tag.TagKey;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
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
			PaletteStorage storage = context.storage();
			int fromID   = context.id(this.replace);
			int toID     = context.id(this.place);
			if (storage != (storage = context.storage())) { //resize
				fromID   = context.id(this.replace);
				toID     = context.id(this.place);
				assert storage == context.storage();
			}
			return new OneBlockReplacer(fromID, toID);
		}

		public static class OneBlockReplacer implements PaletteIdReplacer {

			public final int from, to;

			public OneBlockReplacer(int from, int to) {
				this.from = from;
				this.to = to;
			}

			@Override
			public int getReplacement(int id) {
				return id == this.from ? this.to : id;
			}
		}
	}
}