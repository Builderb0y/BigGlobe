package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.ColumnZone;
import builderb0y.bigglobe.dynamicRegistries.OverworldBiomeLayout;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;

public class OverworldSettings {
	public final OverworldHeightSettings height;
	public final OverworldTemperatureSettings temperature;
	public final OverworldFoliageSettings foliage;
	public final OverworldSurfaceSettings surface;
	public final Grid2D flower_noise;
	public final OverworldUndergroundSettings underground;
	public final @VerifyNullable OverworldSkylandSettings skylands;
	public final OverworldMiscellaneousSettings miscellaneous;

	public final OverworldBiomeLayout.@EncodeInline Holder biomes;

	public OverworldSettings(
		OverworldHeightSettings height,
		OverworldTemperatureSettings temperature,
		OverworldFoliageSettings foliage,
		OverworldSurfaceSettings surface,
		Grid2D flower_noise,
		OverworldUndergroundSettings underground,
		@VerifyNullable OverworldSkylandSettings skylands,
		OverworldMiscellaneousSettings miscellaneous,
		OverworldBiomeLayout.Holder biomes
	) {
		this.height = height;
		this.temperature = temperature;
		this.foliage = foliage;
		this.surface = surface;
		this.flower_noise = flower_noise;
		this.underground = underground;
		this.skylands = skylands;
		this.miscellaneous = miscellaneous;
		this.biomes = biomes;
	}

	public boolean hasSkylands() {
		return this.skylands != null;
	}

	public static record OverworldTemperatureSettings(
		Grid2D noise
	) {}

	public static record OverworldFoliageSettings(
		Grid2D noise
	) {}

	public static record OverworldSurfaceSettings(
		ColumnZone<@EncodeInline OverworldSurfaceBlocks> blocks,
		@VerifyNullable SortedFeatureTag decorator
	) {

		public static record OverworldSurfaceBlocks(BlockState top, BlockState under) {}
	}

	public static record OverworldMiscellaneousSettings(
		BlockState subsurface_state,
		double beach_y,
		double snow_temperature_multiplier,
		double temperature_height_falloff,
		double foliage_height_falloff,
		double surface_depth_falloff
	) {}
}