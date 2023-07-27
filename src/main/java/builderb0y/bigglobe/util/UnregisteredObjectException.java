package builderb0y.bigglobe.util;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.versions.RegistryEntryListVersions;

/**
sometimes thrown when the {@link Identifier} or {@link RegistryKey} is
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

	public static <T> RegistryKey<T> getKey(Registry<T> registry, T object) {
		RegistryKey<T> key = registry.getKey(object).orElse(null);
		if (key != null) return key;
		else throw new UnregisteredObjectException("Unregistered object " + object + " in " + registry);
	}

	public static <T> RegistryKey<T> getKey(RegistryEntry<T> entry) {
		RegistryKey<T> key = entry.getKey().orElse(null);
		if (key != null) return key;
		else throw new UnregisteredObjectException("Unregistered object: " + entry);
	}

	public static <T> Identifier getID(RegistryEntry<T> entry) {
		return getKey(entry).getValue();
	}

	public static <T> TagKey<T> getTagKey(RegistryEntryList<T> list) {
		TagKey<T> key = RegistryEntryListVersions.getKeyNullable(list);
		if (key != null) return key;
		else throw new UnregisteredObjectException("Unregistered tag key: " + list);
	}
}