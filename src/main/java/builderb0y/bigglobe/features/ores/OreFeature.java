package builderb0y.bigglobe.features.ores;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.scripting.parsing.ScriptParsingException;

public class OreFeature<T_Config extends OreFeature.Config> extends DummyFeature<T_Config> {

	public OreFeature(Class<T_Config> configCodec) {
		super(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(configCodec));
	}

	public static interface PaletteIdReplacer {

		public abstract int getReplacement(int id);
	}

	public static abstract class Config extends DummyConfig {

		public final ColumnYToDoubleScript.Holder chance;
		public final RandomSource radius;

		public Config(ColumnYToDoubleScript.Holder chance, RandomSource radius) throws ScriptParsingException {
			this.radius = radius;
			this.chance = chance;
		}

		public double getChance(WorldColumn column, double y) {
			return this.chance.evaluate(column, y);
		}

		public abstract PaletteIdReplacer getReplacer(SectionGenerationContext context);
	}
}