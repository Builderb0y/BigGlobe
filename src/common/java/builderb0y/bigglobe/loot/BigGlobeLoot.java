package builderb0y.bigglobe.loot;

import builderb0y.bigglobe.BigGlobeMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public class BigGlobeLoot {

	static {
		BigGlobeMod.LOGGER.debug("Registering loot pool entry types...");
	}

	public static final LootPoolEntryType FUNCTIONAL_GROUP = registerEntry("group", FunctionalGroupEntry.SERIALIZER);
	public static final LootItemFunctionType CHOOSE_POTION_TYPE = registerFunction("choose_potion", ChoosePotionLootFunction.SERIALIZER);
	public static final LootItemFunctionType RANDOMIZE_DYE_COLOR = registerFunction("randomize_dye_color", RandomizeDyeColorLootFunction.SERIALIZER);

	static {
		BigGlobeMod.LOGGER.debug("Done registering loot pool entry types.");
	}

	public static LootPoolEntryType registerEntry(String id, LootPoolEntryType serializer) {
		return Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE, BigGlobeMod.modID(id), serializer);
	}

	public static LootItemFunctionType registerFunction(String id, LootItemFunctionType serializer) {
		return Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, BigGlobeMod.modID(id), serializer);
	}

	public static void init() {
	}
}