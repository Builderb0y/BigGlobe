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
		SLIMES_SPAWN_ON_WATER = of("slimes_spawn_on_water"),
		SMALL_DECORATIONS     = of("small_decorations"),
		UNDERGROUND_POCKETS   = of("underground_pockets");

	public static TagKey<Structure> of(String name) {
		return TagKey.of(RegistryKeyVersions.structure(), BigGlobeMod.modID(name));
	}
}