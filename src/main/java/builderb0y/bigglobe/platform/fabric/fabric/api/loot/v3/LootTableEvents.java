package builderb0y.bigglobe.platform.fabric.fabric.api.loot.v3;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class LootTableEvents {

	@FunctionalInterface
	public interface Modify {
		void modify(ResourceKey<LootTable> id, LootTable.Builder tableBuilder, LootTableSource source, HolderLookup.Provider registries);
	}

	public static class Event<T> {
		private final List<T> listeners = new ArrayList<>();

		public void register(T listener) {
			this.listeners.add(listener);
		}

		public List<T> getListeners() {
			return this.listeners;
		}
	}

	public static final Event<Modify> MODIFY = new Event<>();
}
