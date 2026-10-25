package net.fabricmc.fabric.api.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public class ServerLifecycleEvents {

	@FunctionalInterface
	public interface ServerStarting {
		void onServerStarting(MinecraftServer server);
	}

	@FunctionalInterface
	public interface ServerStarted {
		void onServerStarted(MinecraftServer server);
	}

	@FunctionalInterface
	public interface ServerStopping {
		void onServerStopping(MinecraftServer server);
	}

	@FunctionalInterface
	public interface ServerStopped {
		void onServerStopped(MinecraftServer server);
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

	public static final Event<ServerStarting> SERVER_STARTING = new Event<>();
	public static final Event<ServerStarted> SERVER_STARTED = new Event<>();
	public static final Event<ServerStopping> SERVER_STOPPING = new Event<>();
	public static final Event<ServerStopped> SERVER_STOPPED = new Event<>();
}
