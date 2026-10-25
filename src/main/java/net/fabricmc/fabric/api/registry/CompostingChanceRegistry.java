package net.fabricmc.fabric.api.registry;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ComposterBlock;

public interface CompostingChanceRegistry {

	CompostingChanceRegistry INSTANCE = new CompostingChanceRegistry() {
		@Override
		public void add(ItemLike item, float chance) {
			ComposterBlock.COMPOSTABLES.put(item.asItem(), chance);
		}

		@Override
		public Float get(ItemLike item) {
			return ComposterBlock.COMPOSTABLES.getFloat(item.asItem());
		}
	};

	void add(ItemLike item, float chance);

	Float get(ItemLike item);
}
