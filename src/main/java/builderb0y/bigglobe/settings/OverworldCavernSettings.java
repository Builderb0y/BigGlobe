package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;
import net.minecraft.util.registry.Registry;

import builderb0y.autocodec.annotations.*;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;

public class OverworldCavernSettings {

	public final VoronoiDiagram2D placement;
	public final Registry<LocalCavernSettings> template_registry;
	public final IRandomList<LocalCavernSettings> templates;

	public OverworldCavernSettings(VoronoiDiagram2D placement, Registry<LocalCavernSettings> template_registry) {
		this.placement = placement;
		this.template_registry = template_registry;
		this.templates = BigGlobeDynamicRegistries.sortAndCollect(template_registry);
	}

	public static record LocalCavernSettings(
		@DefaultDouble(1.0D) double weight,
		@VerifyFloatRange(min = 0.0D) double padding,
		RandomSource average_center,
		Grid2D center,
		Grid2D thickness,
		@Hidden double sqrtMaxThickness,
		@VerifyNullable BlockState fluid,
		@VerifyNullable SortedFeatureTag floor_decorator,
		@VerifyNullable SortedFeatureTag ceiling_decorator,
		@VerifyNullable SortedFeatureTag fluid_decorator,
		@DefaultBoolean(false) boolean has_ancient_cities
	)
	implements IWeightedListElement {

		public LocalCavernSettings(
			double weight,
			double padding,
			RandomSource center,
			Grid2D center_variation,
			Grid2D thickness,
			@VerifyNullable BlockState fluids,
			@VerifyNullable SortedFeatureTag floor_decorator,
			@VerifyNullable SortedFeatureTag ceiling_decorator,
			@VerifyNullable SortedFeatureTag fluid_decorator,
			boolean has_ancient_cities
		) {
			this(
				weight,
				padding,
				center,
				center_variation,
				thickness,
				Math.sqrt(thickness.maxValue()),
				fluids,
				floor_decorator,
				ceiling_decorator,
				fluid_decorator,
				has_ancient_cities
			);
		}

		@Override
		public double getWeight() {
			return this.weight;
		}
	}
}