package builderb0y.bigglobe.settings;

import java.util.Comparator;

import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.dynamicRegistries.OverworldBiomeLayout.PrimarySurface;
import builderb0y.bigglobe.dynamicRegistries.OverworldBiomeLayout.SecondarySurface;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.randomLists.ContainedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.scripting.SurfaceDepthWithSlopeScript;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class OverworldSkylandSettings {

	public final VoronoiDiagram2D placement;
	public final RegistryWrapper<LocalSkylandSettings> templates_registry;
	public final transient IRandomList<LocalSkylandSettings> templates;

	public OverworldSkylandSettings(VoronoiDiagram2D placement, RegistryWrapper<LocalSkylandSettings> templates_registry) {
		this.placement = placement;
		this.templates_registry = templates_registry;
		this.templates = new ContainedRandomList<>();
		templates_registry
		.streamEntries()
		.sorted(
			Comparator.comparing(
				(RegistryEntry<LocalSkylandSettings> entry) -> (
					UnregisteredObjectException.getKey(entry).getValue()
				),
				Comparator
				.comparing(Identifier::getNamespace)
				.thenComparing(Identifier::getPath)
			)
		)
		.map(RegistryEntry::value)
		.forEachOrdered(this.templates::add);
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