package net.fabricmc.fabric.api.itemgroup.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public class ItemGroupEvents {

	public static class ModifyEntries {
		private final List<Consumer<FabricItemGroupEntries>> listeners = new ArrayList<>();

		public void register(Consumer<FabricItemGroupEntries> listener) {
			this.listeners.add(listener);
		}

		public List<Consumer<FabricItemGroupEntries>> getListeners() {
			return this.listeners;
		}
	}

	private static final Map<ResourceKey<CreativeModeTab>, ModifyEntries> EVENTS = new HashMap<>();

	public static synchronized ModifyEntries modifyEntriesEvent(ResourceKey<CreativeModeTab> tab) {
		return EVENTS.computeIfAbsent(tab, k -> new ModifyEntries());
	}

	public static Map<ResourceKey<CreativeModeTab>, ModifyEntries> getEvents() {
		return EVENTS;
	}
}
