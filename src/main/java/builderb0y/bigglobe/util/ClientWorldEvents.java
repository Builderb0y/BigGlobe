package builderb0y.bigglobe.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class ClientWorldEvents {

	public static final Event<Load> LOAD = EventFactory.createArrayBacked(Load.class, (Load[] events) -> (ClientWorld world) -> {
		for (Load event : events) {
			event.load(world);
		}
	});

	public static final Event<Unload> UNLOAD = EventFactory.createArrayBacked(Unload.class, (Unload[] events) -> () -> {
		for (Unload event : events) {
			event.unload();
		}
	});

	@Environment(EnvType.CLIENT)
	public static interface Load {

		public abstract void load(ClientWorld world);
	}

	@Environment(EnvType.CLIENT)
	public static interface Unload {

		public abstract void unload();
	}
}