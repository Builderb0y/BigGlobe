package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYRandomToDoubleScript;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public class NetherSettings {
	public final VoronoiDiagram2D biome_placement;
	public final RegistryWrapper<LocalNetherSettings> local_settings_registry;
	public final transient IRandomList<LocalNetherSettings> local_settings;
	public final @VerifyDivisibleBy16 int min_y;
	public final @VerifyDivisibleBy16 int max_y;

	public NetherSettings(
		VoronoiDiagram2D biome_placement,
		RegistryWrapper<LocalNetherSettings> local_settings_registry,
		int min_y,
		int max_y
	) {
		this.biome_placement = biome_placement;
		this.local_settings_registry = local_settings_registry;
		this.local_settings = BigGlobeDynamicRegistries.sortAndCollect(local_settings_registry);
		this.min_y = min_y;
		this.max_y = max_y;
	}

	public int height() {
		return this.max_y - this.min_y;
	}

	public static record LocalNetherSettings(
		double weight,
		RegistryEntry<Biome> biome,
		NetherCavernSettings caverns,
		NetherCaveSettings caves,
		BlockState fluid_state,
		RandomSource fluid_level,
		SortedFeatureTag fluid_decorator,
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
		int edge_padding,
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