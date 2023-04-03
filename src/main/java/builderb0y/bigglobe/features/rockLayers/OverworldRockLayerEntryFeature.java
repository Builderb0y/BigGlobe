package builderb0y.bigglobe.features.rockLayers;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer.TwoBlockReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.noise.Grid2D;

public class OverworldRockLayerEntryFeature extends RockLayerEntryFeature<OverworldRockLayerEntryFeature.Entry> {

	public OverworldRockLayerEntryFeature(Codec<Config<Entry>> codec) {
		super(codec);
	}

	public OverworldRockLayerEntryFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(new ReifiedType<>() {}));
	}

	public static class Entry extends RockLayerEntryFeature.Entry {

		public final @VerifyNormal BlockState smooth_state, cobble_state;

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			Grid2D center,
			Grid2D thickness,
			@VerifyNormal BlockState smooth_state,
			@VerifyNormal BlockState cobble_state
		) {
			super(weight, restrictions, center, thickness);
			this.smooth_state = smooth_state;
			this.cobble_state = cobble_state;
		}

		@Override
		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return new TwoBlockReplacer(context, BlockStates.STONE, this.smooth_state, BlockStates.COBBLESTONE, this.cobble_state);
		}
	}
}