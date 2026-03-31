package builderb0y.bigglobe.loot;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeLoot {

	static {
		BigGlobeMod.LOGGER.debug("Registering loot pool entry types...");
	}

	public static final MapCodec<FunctionalGroupEntry> FUNCTIONAL_GROUP = registerEntry("group", FunctionalGroupEntry.CODEC);
	public static final MapCodec<ChoosePotionLootFunction> CHOOSE_POTION_TYPE = registerFunction("choose_potion", ChoosePotionLootFunction.CODEC);
	public static final MapCodec<RandomizeDyeColorLootFunction> RANDOMIZE_DYE_COLOR = registerFunction("randomize_dye_color", RandomizeDyeColorLootFunction.CODEC);

	static {
		BigGlobeMod.LOGGER.debug("Done registering loot pool entry types.");
	}

	public static <T extends LootPoolEntryContainer> MapCodec<T> registerEntry(String id, MapCodec<T> serializer) {
		return Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE, BigGlobeMod.modID(id), serializer);
	}

	public static <T extends LootItemFunction> MapCodec<T> registerFunction(String id, MapCodec<T> serializer) {
		return Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, BigGlobeMod.modID(id), serializer);
	}

	public static void init() {}
}