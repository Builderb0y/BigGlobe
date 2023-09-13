package builderb0y.bigglobe.features.ores;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier.VerifyRandomRange;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;

public class OreFeature<T_Config extends OreFeature.Config> extends DummyFeature<T_Config> {

	public OreFeature(Class<T_Config> configCodec) {
		super(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(configCodec));
	}

	public static abstract class Config extends DummyConfig {

		public final ColumnYToDoubleScript.Holder chance;
		public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 16.0D) RandomSource radius;

		public Config(ColumnYToDoubleScript.Holder chance, RandomSource radius) {
			this.radius = radius;
			this.chance = chance;
		}

		public boolean canSpawnAt(WorldColumn column, int y) {
			return true;
		}

		public double getChance(WorldColumn column, double y) {
			return this.chance.evaluate(column, y);
		}

		public abstract PaletteIdReplacer getReplacer(SectionGenerationContext context);
	}
}