package builderb0y.bigglobe.settings;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
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
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class OverworldSkylandSettings extends DecoratorTagHolder {

	public final VoronoiDiagram2D placement;
	public final BetterRegistry<LocalSkylandSettings> templates_registry;
	public final transient IRandomList<RegistryEntry<LocalSkylandSettings>> templates;

	public OverworldSkylandSettings(
		VoronoiDiagram2D placement,
		BetterRegistry<LocalSkylandSettings> templates_registry,
		BetterRegistry<ConfiguredFeature<?, ?>> configured_feature_lookup
	) {
		super(configured_feature_lookup);
		this.placement = placement;
		this.templates_registry = templates_registry;
		this.templates = BigGlobeDynamicRegistries.sortAndCollect(templates_registry);
		templates_registry.streamEntries().forEach((RegistryEntry<LocalSkylandSettings> entry) -> {
			Identifier baseKey = UnregisteredObjectException.getKey(entry).getValue();
			LocalSkylandSettings settings = entry.value();
			settings.top_decorator    = this.createDecoratorTag(baseKey, "top");
			settings.bottom_decorator = this.createDecoratorTag(baseKey, "bottom");
		});
	}

	@Override
	public String getDecoratorTagPrefix() {
		return "overworld/skylands";
	}

	public static class LocalSkylandSettings implements IWeightedListElement {

		public final @DefaultDouble(1.0D) double weight;
		public final RandomSource average_center;
		public final Grid2D center;
		public final Grid2D thickness;
		public final @VerifyNullable Grid2D auxiliary_noise;
		public final ColumnYToDoubleScript.Holder min_y;
		public final ColumnYToDoubleScript.Holder max_y;
		public final SkylandSurfaceSettings surface;
		public transient SortedFeatureTag top_decorator;
		public transient SortedFeatureTag bottom_decorator;

		public LocalSkylandSettings(
			@DefaultDouble(1.0D) double weight,
			RandomSource average_center,
			Grid2D center,
			Grid2D thickness,
			@VerifyNullable Grid2D auxiliary_noise,
			ColumnYToDoubleScript.Holder min_y,
			ColumnYToDoubleScript.Holder max_y,
			SkylandSurfaceSettings surface
		) {
			this.weight = weight;
			this.average_center = average_center;
			this.center = center;
			this.thickness = thickness;
			this.auxiliary_noise = auxiliary_noise;
			this.min_y = min_y;
			this.max_y = max_y;
			this.surface = surface;
		}

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