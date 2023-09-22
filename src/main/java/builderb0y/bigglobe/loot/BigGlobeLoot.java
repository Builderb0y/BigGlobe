package builderb0y.bigglobe.loot;

import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION < MC_1_20_2
	import net.minecraft.util.JsonSerializer;
	import net.minecraft.loot.entry.LootPoolEntry;
	import net.minecraft.loot.function.LootFunction;
#endif

public class BigGlobeLoot {

	static { BigGlobeMod.LOGGER.debug("Registering loot pool entry types..."); }

	public static final LootPoolEntryType FUNCTIONAL_GROUP = registerEntry("group", FunctionalGroupEntry.SERIALIZER);
	public static final LootFunctionType CHOOSE_POTION_TYPE = registerFunction("choose_potion", ChoosePotionLootFunction.SERIALIZER);

	static { BigGlobeMod.LOGGER.debug("Done registering loot pool entry types."); }

	#if MC_VERSION >= MC_1_20_2
		public static LootPoolEntryType registerEntry(String id, LootPoolEntryType serializer) {
			return Registry.register(RegistryVersions.lootPoolEntryType(), BigGlobeMod.modID(id), serializer);
		}

		public static LootFunctionType registerFunction(String id, LootFunctionType serializer) {
			return Registry.register(RegistryVersions.lootFunctionType(), BigGlobeMod.modID(id), serializer);
		}
	#else
		public static LootPoolEntryType registerEntry(String id, JsonSerializer<? extends LootPoolEntry> serializer) {
			return Registry.register(RegistryVersions.lootPoolEntryType(), BigGlobeMod.modID(id), new LootPoolEntryType(serializer));
		}

		public static LootFunctionType registerFunction(String id, JsonSerializer<? extends LootFunction> serializer) {
			return Registry.register(RegistryVersions.lootFunctionType(), BigGlobeMod.modID(id), new LootFunctionType(serializer));
		}
	#endif

	public static void init() {}
}