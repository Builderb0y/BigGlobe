package builderb0y.bigglobe.platform.fabric.fabric.api.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;

public class ServerTickEvents {

	public static class WorldTickEvent {
		public final List<Consumer<ServerLevel>> listeners = new ArrayList<>();

		public void register(Consumer<ServerLevel> listener) {
			this.listeners.add(listener);
		}
	}

	public static final WorldTickEvent END_WORLD_TICK = new WorldTickEvent();
}
