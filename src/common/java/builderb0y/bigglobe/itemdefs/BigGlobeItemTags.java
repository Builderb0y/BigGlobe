package builderb0y.bigglobe.itemdefs;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.IdentifierVersions;

public class BigGlobeItemTags {

	public static final TagKey<Item>
		//common
		BUDDING_BLOCKS = common("budding_blocks"),
		BUDS = common("buds"),
		CLUSTERS = common("clusters"),
		FEATHERS = common("feathers"),
		STRING = common("string"),
		SULFUR_ORES = common("ores/sulfur"),
		SULFURS = common("sulfurs"),

	AURA_BOTTLES = of("aura_bottles"),
		CHARRED_LOGS = of("charred_logs"),
		SLINGSHOT_AMMUNITION = of("slingshot_ammunition"),
		SOLID_AURA_BOTTLES = of("solid_aura_bottles"),
		REPAIRS_VOIDMETAL_ARMOR = of("repairs_voidmetal_armor");

	public static TagKey<Item> of(String name) {
		return TagKey.create(Registries.ITEM, BigGlobeMod.modID(name));
	}

	public static TagKey<Item> common(String name) {
		return TagKey.create(Registries.ITEM, IdentifierVersions.create("c", name));
	}
}