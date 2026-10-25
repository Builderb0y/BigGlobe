package net.fabricmc.fabric.api.loot.v3;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public interface LootTableSource {
	boolean isBuiltin();
}
