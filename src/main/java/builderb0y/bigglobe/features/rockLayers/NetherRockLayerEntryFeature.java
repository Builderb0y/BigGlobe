package builderb0y.bigglobe.features.rockLayers;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer.OneBlockReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.noise.Grid2D;

public class NetherRockLayerEntryFeature extends RockLayerEntryFeature<NetherRockLayerEntryFeature.Entry> {

	public NetherRockLayerEntryFeature(Codec<Config<Entry>> codec) {
		super(codec);
	}

	public NetherRockLayerEntryFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(new ReifiedType<>() {}));
	}

	public static class Entry extends RockLayerEntryFeature.Entry {

		public final @VerifyNormal BlockState place;
		public final @VerifyNormal BlockState replace;

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			Grid2D center,
			Grid2D thickness,
			@VerifyNormal BlockState place,
			@VerifyNormal BlockState replace
		) {
			super(weight, restrictions, center, thickness);
			this.place = place;
			this.replace = replace;
		}

		@Override
		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return new OneBlockReplacer(context, this.replace, this.place);
		}
	}
}