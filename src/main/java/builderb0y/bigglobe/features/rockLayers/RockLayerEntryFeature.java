package builderb0y.bigglobe.features.rockLayers;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.features.LinkedConfig;
import builderb0y.bigglobe.features.LinkedConfig.EntryConfig;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.settings.VariationsList;

public class RockLayerEntryFeature extends DummyFeature<RockLayerEntryFeature.Config> {

	public RockLayerEntryFeature(Codec<Config> codec) {
		super(codec);
	}

	public RockLayerEntryFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends EntryConfig<Entry> {

		public Config(Identifier group, VariationsList<Entry> entries) {
			super(group, entries);
		}
	}

	public static class Entry extends LinkedConfig.Entry {

		public final @VerifyNormal BlockState smooth_state, cobble_state;
		public final Grid2D center, thickness;

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			@VerifyNormal BlockState smooth_state,
			@VerifyNormal BlockState cobble_state,
			Grid2D center,
			Grid2D thickness
		) {
			super(weight, restrictions);
			this.smooth_state = smooth_state;
			this.cobble_state = cobble_state;
			this.center       = center;
			this.thickness    = thickness;
		}
	}
}