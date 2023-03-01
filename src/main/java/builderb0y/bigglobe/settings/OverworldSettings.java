package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.ColumnZone;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;

public record OverworldSettings(
	OverworldHeightSettings height,
	OverworldTemperatureSettings temperature,
	OverworldFoliageSettings foliage,
	OverworldSurfaceSettings surface,
	Grid2D flower_noise,
	OverworldUndergroundSettings underground,
	@VerifyNullable OverworldSkylandSettings skylands,
	OverworldMiscellaneousSettings miscellaneous
) {

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
		ColumnZone<@UseName("biome") RegistryEntry<Biome>> biomes,
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