package builderb0y.bigglobe.versions;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class TagKeyVersions {

	@SuppressWarnings("unchecked")
	public static <T> ResourceKey<Registry<T>> registry(TagKey<T> key) {

		return (ResourceKey<Registry<T>>)(key.registry());
	}
}