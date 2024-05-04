package builderb0y.bigglobe.versions;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

public class RegistryEntryListVersions {

	public static <T> Optional<TagKey<T>> getKeyOptional(RegistryEntryList<T> list) {
		return list.getTagKey();
	}

	public static <T> @Nullable TagKey<T> getKeyNullable(RegistryEntryList<T> list) {
		return getKeyOptional(list).orElse(null);
	}

	public static <T> TagKey<T> getKeyOrThrow(RegistryEntryList<T> list) {
		TagKey<T> key = getKeyNullable(list);
		if (key != null) return key;
		else throw new IllegalArgumentException(list + " has no key!");
	}
}