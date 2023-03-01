package builderb0y.bigglobe.registration;

import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeBiomeTags {

	public static final TagKey<Biome>
		COLD                  = of("cold"),
		TEMPERATE             = of("temperate"),
		HOT                   = of("hot"),
		FOREST                = of("forest"),
		PLAINS                = of("plains"),
		WASTELAND             = of("wasteland"),
		LAND                  = of("land"),
		OCEAN                 = of("ocean"),
		OVERWORLD             = of("overworld"),
		PLAYER_SPAWN_FRIENDLY = of("player_spawn_friendly");

	public static TagKey<Biome> of(String name) {
		return TagKey.of(Registry.BIOME_KEY, BigGlobeMod.modID(name));
	}
}