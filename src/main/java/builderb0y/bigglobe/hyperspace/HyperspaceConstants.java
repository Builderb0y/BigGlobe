package builderb0y.bigglobe.hyperspace;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class HyperspaceConstants {

	public static final RegistryKey<World> WORLD_KEY = RegistryKey.of(RegistryKeyVersions.world(), BigGlobeMod.modID("hyperspace"));
	public static final RegistryKey<DimensionOptions> DIMENSION_OPTIONS_KEY = RegistryKey.of(RegistryKeyVersions.dimension(), BigGlobeMod.modID("hyperspace"));
	public static final RegistryKey<Biome> BIOME_KEY = RegistryKey.of(RegistryKeyVersions.biome(), BigGlobeMod.modID("hyperspace"));
	public static final int
		DIMENSION_MIN_Y = 0,
		DIMENSION_MAX_Y = 16,
		DIMENSION_HEIGHT = DIMENSION_MAX_Y - DIMENSION_MIN_Y;
}