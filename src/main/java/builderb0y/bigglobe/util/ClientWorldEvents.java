package builderb0y.bigglobe.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class ClientWorldEvents {

	/**
	fired when a world is loaded on the client.
	this event is fired as late as possible,
	after {@link MinecraftClient#world} is updated.
	*/
	public static final Event<Load> LOAD = EventFactory.createArrayBacked(Load.class, (Load[] events) -> (ClientWorld world) -> {
		for (Load event : events) {
			event.load(world);
		}
	});

	/**
	fired when a world is unloaded on the client.
	this event is fired as early as possible,
	before {@link MinecraftClient#world} is updated.
	*/
	public static final Event<Unload> UNLOAD = EventFactory.createArrayBacked(Unload.class, (Unload[] events) -> (ClientWorld world) -> {
		for (Unload event : events) {
			event.unload(world);
		}
	});

	@Environment(EnvType.CLIENT)
	public static interface Load {

		public abstract void load(ClientWorld world);
	}

	@Environment(EnvType.CLIENT)
	public static interface Unload {

		public abstract void unload(ClientWorld world);
	}
}