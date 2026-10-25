package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class ClientTickEvents {

	public interface EndTick {
		void onEndTick(Minecraft client);
	}

	public static class ClientTickEventHolder {
		public final List<EndTick> listeners = new ArrayList<>();

		public void register(EndTick listener) {
			this.listeners.add(listener);
		}
	}

	public static class ClientWorldTickEventHolder {
		public final List<Consumer<ClientLevel>> listeners = new ArrayList<>();

		public void register(Consumer<ClientLevel> listener) {
			this.listeners.add(listener);
		}
	}

	public static final ClientTickEventHolder END_CLIENT_TICK = new ClientTickEventHolder();
	public static final ClientWorldTickEventHolder START_WORLD_TICK = new ClientWorldTickEventHolder();
}
