package builderb0y.bigglobe.items;

import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class BigGlobeItemTags {

	public static final TagKey<Item>
		//common
		BLACK_DYES           = common("black_dyes"),
		BUDDING_BLOCKS       = common("budding_blocks"),
		BUDS                 = common("buds"),
		CHARCOAL             = common("charcoal"),
		CLUSTERS             = common("clusters"),
		COAL                 = common("coal"),
		DUSTS                = common("dusts"),
		FEATHERS             = common("feathers"),
		IRON_INGOTS          = common("iron_ingots"),
		STRING               = common("string"),
		SULFUR_ORES          = common("sulfur_ores"),
		SULFURS              = common("sulfurs"),
		WOOD_STICKS          = common("wood_sticks"),
		//general
		AURA_BOTTLES         = of("aura_bottles"),
		CHARRED_LOGS         = of("charred_logs"),
		SLINGSHOT_AMMUNITION = of("slingshot_ammunition"),
		SOLID_AURA_BOTTLES   = of("solid_aura_bottles");

	public static TagKey<Item> of(String name) {
		return TagKey.of(RegistryKeyVersions.item(), BigGlobeMod.modID(name));
	}

	public static TagKey<Item> common(String name) {
		return TagKey.of(RegistryKeyVersions.item(), IdentifierVersions.create("c", name));
	}
}