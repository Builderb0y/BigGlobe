package builderb0y.bigglobe.features.flowers;

import com.mojang.serialization.Codec;

import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.features.LinkedConfig;
import builderb0y.bigglobe.features.LinkedConfig.EntryConfig;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.settings.VariationsList;

public class FlowerEntryFeature extends DummyFeature<FlowerEntryFeature.Config> {

	public FlowerEntryFeature(Codec<Config> codec) {
		super(codec);
	}

	public FlowerEntryFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends EntryConfig<Entry> {

		public Config(Identifier group, VariationsList<Entry> entries) {
			super(group, entries);
		}
	}

	public static class Entry extends LinkedConfig.Entry {

		public final RandomSource radius;
		public final SingleBlockFeature.Config state;
		public final SingleBlockFeature.@VerifyNullable Config under;

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			RandomSource radius,
			SingleBlockFeature.Config state,
			SingleBlockFeature.@VerifyNullable Config under
		) {
			super(weight, restrictions);
			this.radius = radius;
			this.state  = state;
			this.under  = under;
		}
	}
}