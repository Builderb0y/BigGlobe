package builderb0y.bigglobe.features.ores;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
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
		public final @VerifyNullable BlockState2ObjectMap<@VerifyNormal BlockState> blocks;

		public Config(ColumnYToDoubleScript.Holder chance, RandomSource radius, BlockState2ObjectMap<BlockState> blocks) {
			this.radius = radius;
			this.chance = chance;
			this.blocks = blocks != null ? blocks : new BlockState2ObjectMap<>();
		}

		public boolean canSpawnAt(WorldColumn column, int y) {
			return true;
		}

		public double getChance(WorldColumn column, double y) {
			return this.chance.evaluate(column, y);
		}

		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return PaletteIdReplacer.of(context, this.blocks);
		}
	}
}