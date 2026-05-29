package builderb0y.bigglobe.structures;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.StructureType;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeStructureTypeTags {

	public static final TagKey<StructureType<?>>
		CAVE_EXCLUSION_CUBOID = of("cave_exclusion_cuboid"),
		CAVE_EXCLUSION_CYLINDER = of("cave_exclusion_cylinder"),
		CAVE_EXCLUSION_SPHERE = of("cave_exclusion_sphere"),
		RESTRICT_TO_BIOME = of("restrict_to_biome"),
		UNDERGROUND = of("underground");

	public static TagKey<StructureType<?>> of(String name) {
		return TagKey.create(Registries.STRUCTURE_TYPE, BigGlobeMod.modID(name));
	}
}