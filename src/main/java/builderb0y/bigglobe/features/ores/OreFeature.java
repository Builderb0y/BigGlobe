package builderb0y.bigglobe.features.ores;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Palette;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer.ManyBlockReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier.VerifyRandomRange;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class OreFeature<T_Config extends OreFeature.Config> extends DummyFeature<T_Config> {

	public OreFeature(Class<T_Config> configCodec) {
		super(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(configCodec));
	}

	public static class Config extends DummyConfig {

		public final ColumnYToDoubleScript.Holder chance;
		public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 16.0D) RandomSource radius;
		public final BlockState2ObjectMap<BlockState> blocks;

		public Config(ColumnYToDoubleScript.Holder chance, RandomSource radius, BlockState2ObjectMap<BlockState> blocks) {
			this.radius = radius;
			this.chance = chance;
			this.blocks = blocks;
		}

		public boolean canSpawnAt(WorldColumn column, int y) {
			return true;
		}

		public double getChance(WorldColumn column, double y) {
			return this.chance.evaluate(column, y);
		}

		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			outer:
			while (true) {
				Palette<BlockState> palette = context.palette();
				PaletteStorage storage = context.storage();
				int size = palette.getSize();
				int[] lookup = new int[size];
				for (int id = 0; id < size; id++) {
					BlockState from = palette.get(id);
					BlockState to = this.blocks.runtimeStates.get(from);
					lookup[id] = to != null ? palette.index(to) : id;
					//note: it is important to check for a resize after every iteration
					//in this specific method, because of our use of a loop.
					//a resize could change the palette size, which would change our loop bound.
					//we don't want to call palette.get(id) with an invalid id,
					//because that would throw an exception.
					if (storage != (storage = context.storage())) { //resize occurred. start over.
						continue outer;
					}
				}
				return new ManyBlockReplacer(lookup);
			}
		}
	}
}