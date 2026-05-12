package builderb0y.bigglobe.versions;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import builderb0y.bigglobe.util.Grouper;

public class RegistryVersions {

	@SuppressWarnings("unchecked")
	public static <T> ResourceKey<Registry<T>> getRegistryKey(Registry<T> registry) {
		return (ResourceKey<Registry<T>>)(registry.key());
	}

	public static <T> ResourceKey<Registry<T>> getRegistryKey(ResourceKey<T> key) {

		return key.registryKey();
	}

	@SuppressWarnings("unchecked")
	public static <T> ResourceKey<Registry<T>> getRegistryKey(TagKey<T> key) {

		return (ResourceKey<Registry<T>>)(key.registry());
	}

	@SuppressWarnings("unchecked")
	public static <T> ResourceKey<Registry<T>> getRegistryKey(HolderLookup.RegistryLookup<T> registry) {

		return (ResourceKey<Registry<T>>)(registry.key());
	}

	public static <T> T getObject(RegistryAccess manager, ResourceKey<T> key) {

		return manager.lookupOrThrow(getRegistryKey(key)).getValue(key);
	}

	public static <T> Holder<T> getEntry(RegistryAccess manager, ResourceKey<T> key) {

		return manager.lookupOrThrow(getRegistryKey(key)).getOrThrow(key);
	}

	public static <T> Holder<T> getEntry(Registry<T> registry, ResourceKey<T> key) {

		return registry.getOrThrow(key);
	}

	public static <T> Holder<T> getEntry(Registry<T> registry, T object) {
		return registry.wrapAsHolder(object);
	}

	public static <T> Registry<T> getRegistry(RegistryAccess manager, ResourceKey<Registry<T>> key) {

		return manager.lookupOrThrow(key);
	}

	public static <T> HolderSet<T> getTagNullable(Registry<T> registry, TagKey<T> key) {

		return registry.get(key).orElse(null);
	}

	public static <T> HolderSet<T> getTagNullable(RegistryAccess manager, TagKey<T> key) {

		return manager.lookupOrThrow(key.registry()).get(key).orElse(null);
	}

	public static <T> Stream<HolderSet<T>> streamTags(Registry<T> registry) {

		return Grouper.castStream(registry.getTags());
	}
}