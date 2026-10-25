package net.fabricmc.fabric.api.event.registry;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class FabricRegistryBuilder<T> {

	private final MappedRegistry<T> registry;

	public FabricRegistryBuilder(MappedRegistry<T> registry) {
		this.registry = registry;
	}

	public static <T> FabricRegistryBuilder<T> from(MappedRegistry<T> registry) {
		return new FabricRegistryBuilder<>(registry);
	}

	@SuppressWarnings("unchecked")
	public MappedRegistry<T> buildAndRegister() {
		try {
			Registry.register((Registry<Registry<?>>) (Object) BuiltInRegistries.REGISTRY, this.registry.key().location(), this.registry);
		}
		catch (Throwable ignored) {}
		return this.registry;
	}
}
