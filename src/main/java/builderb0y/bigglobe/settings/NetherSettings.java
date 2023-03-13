package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYRandomToDoubleScript;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public record NetherSettings(
	VoronoiDiagram2D biome_placement,
	VariationsList<LocalNetherSettings> local_settings,
	@VerifyDivisibleBy16 int min_y,
	@VerifyDivisibleBy16 int max_y
) {

	public int height() {
		return this.max_y - this.min_y;
	}

	public static record LocalNetherSettings(
		double weight,
		RegistryEntry<Biome> biome,
		NetherCavernSettings caverns,
		NetherCaveSettings caves,
		RandomSource lava_level,
		@VerifyNormal BlockState filler
	)
	implements IWeightedListElement {

		@Override
		public double getWeight() {
			return this.weight;
		}
	}

	public static record NetherCavernSettings(
		int min_y,
		int max_y,
		int lower_padding,
		int upper_padding,
		int side_padding,
		Grid3D noise,
		@VerifyNullable NetherSurfaceSettings floor_surface,
		@VerifyNullable NetherSurfaceSettings ceiling_surface,
		SortedFeatureTag floor_decorator,
		SortedFeatureTag ceiling_decorator
	) {}

	public static record NetherCaveSettings(
		Grid3D noise,
		ColumnYToDoubleScript.Holder width,
		@VerifyNullable Integer lower_padding,
		@VerifyNullable NetherSurfaceSettings floor_surface,
		@VerifyNullable NetherSurfaceSettings ceiling_surface,
		SortedFeatureTag floor_decorator,
		SortedFeatureTag ceiling_decorator
	) {}

	public static record NetherSurfaceSettings(
		BlockState top_state,
		BlockState under_state,
		ColumnYRandomToDoubleScript.Holder depth
	) {}
}