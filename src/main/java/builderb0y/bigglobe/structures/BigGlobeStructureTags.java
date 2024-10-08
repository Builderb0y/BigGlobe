package builderb0y.bigglobe.structures;

import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class BigGlobeStructureTags {

	public static final TagKey<Structure>
		DUNGEONS              = of("dungeons"),
		GEODES                = of("geodes"),
		LAKES                 = of("lakes"),
		MEGA_TREES            = of("mega_trees"),
		NETHER_PILLARS        = of("nether_pillars"),
		RESTRICT_TO_BIOME     = of("restrict_to_biome"),
		SLIMES_SPAWN_ON_WATER = of("slimes_spawn_on_water"),
		SMALL_DECORATIONS     = of("small_decorations"),
		UNDERGROUND           = of("underground"),
		UNDERGROUND_POCKETS   = of("underground_pockets");

	public static TagKey<Structure> of(String name) {
		return TagKey.of(RegistryKeyVersions.structure(), BigGlobeMod.modID(name));
	}
}