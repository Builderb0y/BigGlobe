package builderb0y.bigglobe.versions;

import net.minecraft.resources.ResourceLocation;

public class IdentifierVersions {

	public static ResourceLocation create(String namespace, String path) {

		return ResourceLocation.fromNamespaceAndPath(namespace, path);
	}

	public static ResourceLocation create(String combined) {

		return ResourceLocation.parse(combined);
	}

	public static ResourceLocation vanilla(String path) {

		return ResourceLocation.withDefaultNamespace(path);
	}
}