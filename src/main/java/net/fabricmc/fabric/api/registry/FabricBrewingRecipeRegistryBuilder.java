package net.fabricmc.fabric.api.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.alchemy.PotionBrewing;

public class FabricBrewingRecipeRegistryBuilder {

	public static class BuildEvent {
		private final List<Consumer<PotionBrewing.Builder>> listeners = new ArrayList<>();

		public void register(Consumer<PotionBrewing.Builder> listener) {
			this.listeners.add(listener);
		}

		public List<Consumer<PotionBrewing.Builder>> getListeners() {
			return this.listeners;
		}
	}

	public static final BuildEvent BUILD = new BuildEvent();
}
