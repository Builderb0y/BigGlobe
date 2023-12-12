package builderb0y.bigglobe.features.rockLayers;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class OverworldRockLayerEntryFeature extends RockLayerEntryFeature<OverworldRockLayerEntryFeature.Entry> {

	public OverworldRockLayerEntryFeature(Codec<Config<Entry>> codec) {
		super(codec);
	}

	public OverworldRockLayerEntryFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(new ReifiedType<>() {}));
	}

	@AddPseudoField("smooth_state")
	@AddPseudoField("cobble_state")
	public static class Entry extends RockLayerEntryFeature.Entry {

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			Grid2D center,
			Grid2D thickness,
			@VerifyNormal BlockState smooth_state,
			@VerifyNormal BlockState cobble_state,
			BlockState2ObjectMap<BlockState> blocks
		) {
			super(weight, restrictions, center, thickness, blocks);
			try {
				if (smooth_state != null) this.blocks.addSerialized("minecraft:stone", smooth_state);
				if (cobble_state != null) this.blocks.addSerialized("minecraft:cobblestone", cobble_state);
			}
			catch (CommandSyntaxException exception) {
				throw new AssertionError("did minecraft change the name of stone or cobblestone?", exception);
			}
		}

		@Override
		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return PaletteIdReplacer.of(context, this.blocks);
		}

		//backwards compatibility.

		public @VerifyNormal @VerifyNullable BlockState smooth_state() {
			return null;
		}

		public @VerifyNormal @VerifyNullable BlockState cobble_state() {
			return null;
		}
	}
}