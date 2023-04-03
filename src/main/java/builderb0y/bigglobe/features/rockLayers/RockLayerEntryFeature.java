package builderb0y.bigglobe.features.rockLayers;

import com.mojang.serialization.Codec;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.features.LinkedConfig;
import builderb0y.bigglobe.features.LinkedConfig.EntryConfig;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.settings.VariationsList;

public abstract class RockLayerEntryFeature<T_Entry extends RockLayerEntryFeature.Entry> extends DummyFeature<RockLayerEntryFeature.Config<T_Entry>> {

	public RockLayerEntryFeature(Codec<Config<T_Entry>> codec) {
		super(codec);
	}

	public static class Config<T_Entry extends LinkedConfig.Entry> extends EntryConfig<T_Entry> {

		public Config(Identifier group, VariationsList<T_Entry> entries) {
			super(group, entries);
		}
	}

	public static abstract class Entry extends LinkedConfig.Entry {

		public final Grid2D center, thickness;

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			Grid2D center,
			Grid2D thickness
		) {
			super(weight, restrictions);
			this.center    = center;
			this.thickness = thickness;
		}

		public abstract PaletteIdReplacer getReplacer(SectionGenerationContext context);
	}
}