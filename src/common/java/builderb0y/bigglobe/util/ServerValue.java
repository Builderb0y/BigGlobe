package builderb0y.bigglobe.util;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;

import net.minecraft.server.MinecraftServer;

public class ServerValue<T> implements ServerStopped, Supplier<T> {

	public final Supplier<T> supplier;
	public T value;

	public ServerValue(Supplier<T> supplier) {
		this.supplier = supplier;
		ServerLifecycleEvents.SERVER_STOPPED.register(this);
	}

	@Override
	public T get() {
		T value = this.value;
		if (value == null) {
			value = this.value = this.supplier.get();
		}
		return value;
	}

	@Override
	public void onServerStopped(MinecraftServer server) {
		this.value = null;
	}
}