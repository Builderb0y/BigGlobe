package net.fabricmc.fabric.api.entity.event.v1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ServerEntityWorldChangeEvents {

	public interface AfterPlayerWorldChange {
		void afterChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination);
	}

	public static class EventHolder {
		public final List<AfterPlayerWorldChange> listeners = new ArrayList<>();

		public void register(AfterPlayerWorldChange listener) {
			this.listeners.add(listener);
		}
	}

	public static final EventHolder AFTER_PLAYER_CHANGE_WORLD = new EventHolder();
}
