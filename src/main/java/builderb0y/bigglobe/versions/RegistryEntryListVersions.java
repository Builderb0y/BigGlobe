package builderb0y.bigglobe.versions;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

public class RegistryEntryListVersions {

	public static <T> Optional<TagKey<T>> getKeyOptional(RegistryEntryList<T> list) {
		#if MC_VERSION <= MC_1_19_2
			return list.getStorage().left();
		#else
			return list.getTagKey();
		#endif
	}

	public static <T> @Nullable TagKey<T> getKeyNullable(RegistryEntryList<T> list) {
		return getKeyOptional(list).orElse(null);
	}
}