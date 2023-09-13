package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;
import builderb0y.bigglobe.scripting.interfaces.HeightAdjustmentScript;
import builderb0y.bigglobe.scripting.interfaces.SurfaceDepthWithSlopeScript;
import builderb0y.bigglobe.settings.BiomeLayout.OverworldBiomeLayout;

public class OverworldSettings {

	public final OverworldHeightSettings height;
	public final OverworldTemperatureSettings temperature;
	public final OverworldFoliageSettings foliage;
	public final OverworldMagicalnessSettings magicalness;
	public final OverworldSurfaceSettings surface;
	public final @VerifyNullable OverworldGlacierSettings glaciers;
	public final OverworldUndergroundSettings underground;
	public final @VerifyNullable OverworldSkylandSettings skylands;

	public final BiomeLayout.Holder<OverworldBiomeLayout> biomes;

	public OverworldSettings(
		OverworldHeightSettings height,
		OverworldTemperatureSettings temperature,
		OverworldFoliageSettings foliage,
		OverworldMagicalnessSettings magicalness,
		OverworldSurfaceSettings surface,
		@VerifyNullable OverworldGlacierSettings glaciers,
		OverworldUndergroundSettings underground,
		@VerifyNullable OverworldSkylandSettings skylands,
		BiomeLayout.Holder<OverworldBiomeLayout> biomes
	) {
		this.height      = height;
		this.temperature = temperature;
		this.foliage     = foliage;
		this.magicalness = magicalness;
		this.surface     = surface;
		this.glaciers    = glaciers;
		this.underground = underground;
		this.skylands    = skylands;
		this.biomes      = biomes;
	}

	public boolean hasSkylands() {
		return this.skylands != null;
	}

	public static record OverworldTemperatureSettings(
		Grid2D noise,
		HeightAdjustmentScript.TemperatureHolder height_adjustment
	) {}

	public static record OverworldFoliageSettings(
		Grid2D noise,
		HeightAdjustmentScript.FoliageHolder height_adjustment
	) {}

	public static record OverworldMagicalnessSettings(
		Grid2D noise
	) {}

	public static record OverworldSurfaceSettings(
		SurfaceDepthWithSlopeScript.Holder primary_surface_depth
	) {}

	public static record OverworldGlacierSettings(
		VoronoiDiagram2D cracks,
		ColumnYToDoubleScript.Holder crack_threshold,
		Grid2D bottom_height,
		Grid2D top_height,
		IRandomList<@UseName("state") BlockState>[] states
	) {}
}