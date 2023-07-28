package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYRandomToDoubleScript;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class NetherSettings extends DecoratorTagHolder {
	public final VoronoiDiagram2D biome_placement;
	public final BetterRegistry<LocalNetherSettings> localSettingsRegistry;
	public final transient IRandomList<RegistryEntry<LocalNetherSettings>> local_settings;
	public final @VerifyDivisibleBy16 int min_y;
	public final @VerifyDivisibleBy16 int max_y;

	public NetherSettings(
		VoronoiDiagram2D biome_placement,
		BetterRegistry<LocalNetherSettings> localSettingsRegistry,
		BetterRegistry<ConfiguredFeature<?, ?>> configured_feature_lookup,
		int min_y,
		int max_y
	) {
		super(configured_feature_lookup);
		this.biome_placement = biome_placement;
		this.localSettingsRegistry = localSettingsRegistry;
		this.local_settings = BigGlobeDynamicRegistries.sortAndCollect(localSettingsRegistry);
		this.min_y = min_y;
		this.max_y = max_y;
		localSettingsRegistry.streamEntries().sequential().forEach((RegistryEntry<LocalNetherSettings> localSettingsEntry) -> {
			Identifier baseKey = UnregisteredObjectException.getKey(localSettingsEntry).getValue();
			LocalNetherSettings localSettings = localSettingsEntry.value();
			localSettings.caveCeilingsDecorator   = this.createDecoratorTag(baseKey, "cave_ceilings");
			localSettings.caveFloorsDecorator     = this.createDecoratorTag(baseKey, "cave_floors");
			localSettings.cavernCeilingsDecorator = this.createDecoratorTag(baseKey, "cavern_ceilings");
			localSettings.cavernFloorsDecorator   = this.createDecoratorTag(baseKey, "cavern_floors");
			localSettings.fluidDecorator          = this.createDecoratorTag(baseKey, "fluid");
			localSettings.lowerBedrockDecorator   = this.createDecoratorTag(baseKey, "lower_bedrock");
			localSettings.upperBedrockDecorator   = this.createDecoratorTag(baseKey, "upper_bedrock");
		});
	}

	@Override
	public String getDecoratorTagPrefix() {
		return "nether/biomes";
	}

	public int height() {
		return this.max_y - this.min_y;
	}

	public static class LocalNetherSettings implements IWeightedListElement {

		public final double weight;
		public final RegistryEntry<Biome> biome;
		public final NetherCavernSettings caverns;
		public final NetherCaveSettings caves;
		public final BlockState fluid_state;
		public final RandomSource fluid_level;
		public final @VerifyNormal BlockState filler;

		public transient SortedFeatureTag
			caveCeilingsDecorator,
			caveFloorsDecorator,
			cavernCeilingsDecorator,
			cavernFloorsDecorator,
			fluidDecorator,
			lowerBedrockDecorator,
			upperBedrockDecorator;

		public LocalNetherSettings(
			double weight,
			RegistryEntry<Biome> biome,
			NetherCavernSettings caverns,
			NetherCaveSettings caves,
			BlockState fluid_state,
			RandomSource fluid_level,
			@VerifyNormal BlockState filler
		) {
			this.weight = weight;
			this.biome = biome;
			this.caverns = caverns;
			this.caves = caves;
			this.fluid_state = fluid_state;
			this.fluid_level = fluid_level;
			this.filler = filler;
		}

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
		@VerifyNullable NetherSurfaceSettings ceiling_surface
	) {}

	public static record NetherCaveSettings(
		Grid3D noise,
		ColumnYToDoubleScript.Holder noise_threshold,
		ColumnYToDoubleScript.Holder effective_width,
		@VerifyNullable Integer lower_padding,
		@VerifyNullable NetherSurfaceSettings floor_surface,
		@VerifyNullable NetherSurfaceSettings ceiling_surface
	) {}

	public static record NetherSurfaceSettings(
		BlockState top_state,
		BlockState under_state,
		ColumnYRandomToDoubleScript.Holder depth
	) {}
}