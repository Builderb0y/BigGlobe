package builderb0y.bigglobe.hyperspace;

import builderb0y.bigglobe.BigGlobeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;

public class HyperspaceConstants {

	public static final ResourceKey<Level> WORLD_KEY = ResourceKey.create(Registries.DIMENSION, BigGlobeMod.modID("hyperspace"));
	public static final ResourceKey<LevelStem> DIMENSION_OPTIONS_KEY = ResourceKey.create(Registries.LEVEL_STEM, BigGlobeMod.modID("hyperspace"));
	public static final ResourceKey<Biome> BIOME_KEY = ResourceKey.create(Registries.BIOME, BigGlobeMod.modID("hyperspace"));
}