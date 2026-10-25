package builderb0y.bigglobe.platform.fabric.fabric.api.registry;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public interface FuelRegistry {

	FuelRegistry INSTANCE = new FuelRegistry() {
		private final Map<Item, Integer> fuels = new HashMap<>();

		@Override
		public void add(ItemLike item, int burnTime) {
			this.fuels.put(item.asItem(), burnTime);
		}

		@Override
		public Integer get(ItemLike item) {
			return this.fuels.get(item.asItem());
		}
	};

	void add(ItemLike item, int burnTime);

	Integer get(ItemLike item);
}
