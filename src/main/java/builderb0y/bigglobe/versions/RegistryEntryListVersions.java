package builderb0y.bigglobe.versions;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.RegistryEntryList;

public class RegistryEntryListVersions {

	public static <T> Optional<TagKey<T>> getKeyOptional(RegistryEntryList<T> list) {
		return list.getStorage().left();
	}

	public static <T> @Nullable TagKey<T> getKeyNullable(RegistryEntryList<T> list) {
		return getKeyOptional(list).orElse(null);
	}
}