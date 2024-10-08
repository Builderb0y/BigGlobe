package builderb0y.bigglobe.structures;

import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class BigGlobeStructureTypeTags {

	public static final TagKey<StructureType<?>>
	CAVE_EXCLUSION_CUBOID   = of("cave_exclusion_cuboid"),
	CAVE_EXCLUSION_CYLINDER = of("cave_exclusion_cylinder"),
	CAVE_EXCLUSION_SPHERE   = of("cave_exclusion_sphere"),
	RESTRICT_TO_BIOME       = of("restrict_to_biome"),
	UNDERGROUND             = of("underground");

	public static TagKey<StructureType<?>> of(String name) {
		return TagKey.of(RegistryKeyVersions.structureType(), BigGlobeMod.modID(name));
	}
}