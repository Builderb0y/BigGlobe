package builderb0y.bigglobe.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.multiplayer.ClientLevel;

@Environment(EnvType.CLIENT)
public class ClientWorldEvents {

	public static final Event<WorldChanged> WORLD_CHANGED = EventFactory.createArrayBacked(
		WorldChanged.class, (WorldChanged[] events) -> {
			return (ClientLevel woldWorld, ClientLevel newWorld) -> {
				for (WorldChanged event : events) {
					event.worldChanged(woldWorld, newWorld);
				}
			};
		}
	);

	@Environment(EnvType.CLIENT)
	public static interface WorldChanged {

		public abstract void worldChanged(ClientLevel oldWorld, ClientLevel newWorld);
	}
}