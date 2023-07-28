package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class OverworldCavernSettings extends DecoratorTagHolder {

	public final VoronoiDiagram2D placement;
	public final BetterRegistry<LocalOverworldCavernSettings> template_registry;
	public final IRandomList<RegistryEntry<LocalOverworldCavernSettings>> templates;

	public OverworldCavernSettings(
		VoronoiDiagram2D placement,
		BetterRegistry<LocalOverworldCavernSettings> template_registry,
		BetterRegistry<ConfiguredFeature<?, ?>> configured_feature_lookup
	) {
		super(configured_feature_lookup);
		this.placement = placement;
		this.template_registry = template_registry;
		this.templates = BigGlobeDynamicRegistries.sortAndCollect(template_registry);
		template_registry.streamEntries().sequential().forEach((RegistryEntry<LocalOverworldCavernSettings> entry) -> {
			Identifier baseKey = UnregisteredObjectException.getKey(entry).getValue();
			LocalOverworldCavernSettings settings = entry.value();
			settings.floor_decorator   = this.createDecoratorTag(baseKey, "floor");
			settings.ceiling_decorator = this.createDecoratorTag(baseKey, "ceiling");
			if (settings.fluid != null) settings.fluid_decorator = this.createDecoratorTag(baseKey, "fluid");
		});
	}

	@Override
	public String getDecoratorTagPrefix() {
		return "overworld/caverns";
	}

	public static class LocalOverworldCavernSettings implements IWeightedListElement {

		public final @DefaultDouble(1.0D) double weight;
		public final @VerifyFloatRange(min = 0.0D) double padding;
		public final RandomSource average_center;
		public final Grid2D center;
		public final Grid2D thickness;
		public final @VerifyNullable BlockState fluid;
		public final @DefaultBoolean(false) boolean has_ancient_cities;

		public final transient double sqrtMaxThickness;
		public SortedFeatureTag floor_decorator, ceiling_decorator, fluid_decorator;

		public LocalOverworldCavernSettings(
			@DefaultDouble(1.0D) double weight,
			@VerifyFloatRange(min = 0.0D) double padding,
			RandomSource average_center,
			Grid2D center,
			Grid2D thickness,
			@VerifyNullable BlockState fluid,
			@DefaultBoolean(false) boolean has_ancient_cities
		) {
			this.weight = weight;
			this.padding = padding;
			this.average_center = average_center;
			this.center = center;
			this.thickness = thickness;
			this.fluid = fluid;
			this.has_ancient_cities = has_ancient_cities;
			this.sqrtMaxThickness = Math.sqrt(thickness.maxValue());
		}

		@Override
		public double getWeight() {
			return this.weight;
		}
	}
}