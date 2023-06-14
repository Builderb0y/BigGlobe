package builderb0y.bigglobe.settings;

import net.minecraft.registry.RegistryWrapper;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.scripting.SurfaceDepthWithSlopeScript;
import builderb0y.bigglobe.settings.BiomeLayout.PrimarySurface;
import builderb0y.bigglobe.settings.BiomeLayout.SecondarySurface;

public class OverworldSkylandSettings {

	public final VoronoiDiagram2D placement;
	public final RegistryWrapper<LocalSkylandSettings> templates_registry;
	public final transient IRandomList<LocalSkylandSettings> templates;

	public OverworldSkylandSettings(VoronoiDiagram2D placement, RegistryWrapper<LocalSkylandSettings> templates_registry) {
		this.placement = placement;
		this.templates_registry = templates_registry;
		this.templates = BigGlobeDynamicRegistries.sortAndCollect(templates_registry);
	}

	public static record LocalSkylandSettings(
		@DefaultDouble(1.0D) double weight,
		RandomSource average_center,
		Grid2D center,
		Grid2D thickness,
		@VerifyNullable Grid2D auxiliary_noise,
		ColumnYToDoubleScript.Holder min_y,
		ColumnYToDoubleScript.Holder max_y,
		SkylandSurfaceSettings surface,
		@VerifyNullable SortedFeatureTag floor_decorator,
		@VerifyNullable SortedFeatureTag ceiling_decorator
	)
	implements IWeightedListElement {

		@Override
		public double getWeight() {
			return this.weight;
		}
	}

	public static record SkylandSurfaceSettings(
		PrimarySurface primary,
		SurfaceDepthWithSlopeScript.Holder primary_depth,
		SecondarySurface @VerifyNullable @SingletonArray [] secondary
	) {}
}