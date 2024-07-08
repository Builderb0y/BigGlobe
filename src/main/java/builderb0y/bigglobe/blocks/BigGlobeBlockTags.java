package builderb0y.bigglobe.blocks;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class BigGlobeBlockTags {

	public static final TagKey<Block>
		MINEABLE_PERCUSSIVE_HAMMER          = of("mineable/percussive_hammer"),
		//common
		BUDDING_BLOCKS                      = common("budding_blocks"),
		BUDS                                = common("buds"),
		CLUSTERS                            = common("clusters"),
		END_STONES                          = common("end_stones"),
		FLOWERS                             = common("flowers"),
		GRASS                               = common("grass"),
		GRASS_LIKE                          = common("grass_like"),
		MUSHROOMS                           = common("mushrooms"),
		SULFUR_ORES                         = common("sulfur_ores"),
		//general
		AMETHYST_BUDS                       = of("amethyst_buds"),
		AURA_INFUSED_CLOUDS                 = of("aura_infused_clouds"),
		AURA_INFUSED_VOID_CLOUDS            = of("aura_infused_void_clouds"),
		CHARRED_LOGS                        = of("charred_logs"),
		CLOUDS                              = of("clouds"),
		END_STONE_SPREADABLE                = of("end_stone_spreadable"),
		FARM_STRUCTURE_CROPS                = of("farm_structure_crops"),
		HIDDEN_LAVA_REPLACEABLES_BASALT     = of("hidden_lava_replaceables_basalt"),
		HIDDEN_LAVA_REPLACEABLES_NETHERRACK = of("hidden_lava_replaceables_netherrack"),
		MAGMA_BLOCKS                        = of("magma_blocks"),
		QUARTZ_BUDS                         = of("quartz_buds"),
		ROCK_BREAKABLE                      = of("rock_breakable"),
		ROCK_PLACEABLE_ON_WORLDGEN          = of("rock_placeable_on_worldgen"),
		SMALL_HOUSE_STRUCTURE_FLOWERS       = of("small_house_structure_flowers"),
		SOLID_AURA_INFUSED_CLOUDS           = of("solid_aura_infused_clouds"),
		SOLID_AURA_INFUSED_VOID_CLOUDS      = of("solid_aura_infused_void_clouds"),
		TREE_BRANCH_REPLACEABLES            = of("tree_branch_replaceables"),
		TREE_LEAF_REPLACEABLES              = of("tree_leaf_replaceables"),
		TREE_LOG_REPLACEABLES               = of("tree_log_replaceables"),
		TREE_SHELF_REPLACEABLES             = of("tree_shelf_replaceables"),
		TREE_TRUNK_REPLACEABLES             = of("tree_trunk_replaceables"),
		VALLEY_OF_SOULS_BASE_BLOCKS         = of("valley_of_souls_base_blocks"),
		VOID_CLOUDS                         = of("void_clouds"),
		//version-specific
		PLANTS                              = of("plants");

	public static TagKey<Block> of(String name) {
		return TagKey.of(RegistryKeyVersions.block(), BigGlobeMod.modID(name));
	}

	public static TagKey<Block> common(String name) {
		return TagKey.of(RegistryKeyVersions.block(), IdentifierVersions.create("c", name));
	}
}