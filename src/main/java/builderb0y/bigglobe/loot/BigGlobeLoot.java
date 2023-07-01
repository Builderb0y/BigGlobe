package builderb0y.bigglobe.loot;

import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.JsonSerializer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeLoot {

	static { BigGlobeMod.LOGGER.debug("Registering loot pool entry types..."); }

	public static final LootPoolEntryType FUNCTIONAL_GROUP = registerEntry("group", new FunctionalGroupEntry.Serializer());
	public static final LootFunctionType CHOOSE_POTION_TYPE = registerFunction("choose_potion", new ChoosePotionLootFunction.Serializer());

	static { BigGlobeMod.LOGGER.debug("Done registering loot pool entry types."); }

	public static LootPoolEntryType registerEntry(String id, JsonSerializer<? extends LootPoolEntry> serializer) {
		return Registry.register(RegistryVersions.lootPoolEntryType(), BigGlobeMod.modID(id), new LootPoolEntryType(serializer));
	}

	public static LootFunctionType registerFunction(String id, JsonSerializer<? extends LootFunction> serializer) {
		return Registry.register(RegistryVersions.lootFunctionType(), BigGlobeMod.modID(id), new LootFunctionType(serializer));
	}

	public static void init() {}
}