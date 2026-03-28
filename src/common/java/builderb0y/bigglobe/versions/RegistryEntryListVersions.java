package builderb0y.bigglobe.versions;

import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

public class RegistryEntryListVersions {

	public static <T> Optional<TagKey<T>> getKeyOptional(HolderSet<T> list) {
		return list.unwrapKey();
	}

	public static <T> @Nullable TagKey<T> getKeyNullable(HolderSet<T> list) {
		return getKeyOptional(list).orElse(null);
	}

	public static <T> TagKey<T> getKeyOrThrow(HolderSet<T> list) {
		TagKey<T> key = getKeyNullable(list);
		if (key != null) return key;
		else throw new IllegalArgumentException(list + " has no key!");
	}
}