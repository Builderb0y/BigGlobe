package net.fabricmc.fabric.api.gamerule.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public class GameRuleEvents {

	public static class Callback<T> {
		private final List<BiConsumer<T, MinecraftServer>> listeners = new ArrayList<>();

		public void register(BiConsumer<T, MinecraftServer> listener) {
			this.listeners.add(listener);
		}

		public void fire(T value, MinecraftServer server) {
			for (BiConsumer<T, MinecraftServer> listener : this.listeners) {
				listener.accept(value, server);
			}
		}
	}

	private static final Map<Object, Callback<?>> CALLBACKS = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static <T> Callback<T> changeCallback(Object key) {
		return (Callback<T>)CALLBACKS.computeIfAbsent(key, k -> new Callback<T>());
	}

	@SuppressWarnings("unchecked")
	public static <T> void fire(Object key, T value, MinecraftServer server) {
		Callback<T> cb = (Callback<T>)CALLBACKS.get(key);
		if (cb != null) {
			cb.fire(value, server);
		}
	}
}
