package builderb0y.bigglobe.platform.fabric.fabric.api.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class ServerWorldEvents {

	@FunctionalInterface
	public interface Unload {
		void onWorldUnload(MinecraftServer server, ServerLevel world);
	}

	@FunctionalInterface
	public interface Load {
		void onWorldLoad(MinecraftServer server, ServerLevel world);
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

	public static final Event<Unload> UNLOAD = new Event<>();
	public static final Event<Load> LOAD = new Event<>();
}
