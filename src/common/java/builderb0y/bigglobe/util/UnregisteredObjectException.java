package builderb0y.bigglobe.util;

import builderb0y.bigglobe.versions.RegistryEntryListVersions;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

/**
sometimes thrown when the {@link Identifier} or {@link ResourceKey} is
queried for an object which has not been registered to a {@link Registry}.
*/
public class UnregisteredObjectException extends RuntimeException {

	public UnregisteredObjectException() {}

	public UnregisteredObjectException(String message) {
		super(message);
	}

	public UnregisteredObjectException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnregisteredObjectException(Throwable cause) {
		super(cause);
	}

	public static <T> ResourceKey<T> getKey(Registry<T> registry, T object) {
		ResourceKey<T> key = registry.getResourceKey(object).orElse(null);
		if (key != null) return key;
		else throw new UnregisteredObjectException("Unregistered object " + object + " in " + registry);
	}

	public static <T> Identifier getID(Registry<T> registry, T object) {
		return getKey(registry, object).identifier();
	}

	public static <T> ResourceKey<T> getKey(Holder<T> entry) {
		ResourceKey<T> key = entry.unwrapKey().orElse(null);
		if (key != null) return key;
		else throw new UnregisteredObjectException("Unregistered object: " + entry);
	}

	public static <T> Identifier getID(Holder<T> entry) {
		return getKey(entry).identifier();
	}

	public static <T> TagKey<T> getTagKey(HolderSet<T> list) {
		TagKey<T> key = RegistryEntryListVersions.getKeyNullable(list);
		if (key != null) return key;
		else throw new UnregisteredObjectException("Unregistered tag key: " + list);
	}
}