package builderb0y.bigglobe.features.rockLayers;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.features.LinkedConfig;
import builderb0y.bigglobe.features.LinkedConfig.EntryConfig;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.settings.VariationsList;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public abstract class RockLayerEntryFeature<T_Entry extends RockLayerEntryFeature.Entry> extends DummyFeature<RockLayerEntryFeature.Config<T_Entry>> {

	public RockLayerEntryFeature(Codec<Config<T_Entry>> codec) {
		super(codec);
	}

	public static class Config<T_Entry extends LinkedConfig.Entry> extends EntryConfig<T_Entry> {

		public Config(Identifier group, VariationsList<T_Entry> entries) {
			super(group, entries);
		}
	}

	public static class Entry extends LinkedConfig.Entry {

		public final Grid2D center, thickness;
		public final @VerifyNullable BlockState2ObjectMap<@VerifyNormal BlockState> blocks;

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			Grid2D center,
			Grid2D thickness,
			@VerifyNullable BlockState2ObjectMap<@VerifyNormal BlockState> blocks
		) {
			super(weight, restrictions);
			this.center    = center;
			this.thickness = thickness;
			this.blocks    = blocks != null ? blocks : new BlockState2ObjectMap<>();
		}

		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return PaletteIdReplacer.of(context, this.blocks);
		}
	}
}