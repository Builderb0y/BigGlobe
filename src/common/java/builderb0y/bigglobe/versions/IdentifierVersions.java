package builderb0y.bigglobe.versions;

import net.minecraft.resources.Identifier;

public class IdentifierVersions {

	public static Identifier create(String namespace, String path) {

		return Identifier.fromNamespaceAndPath(namespace, path);
	}

	public static Identifier create(String combined) {

		return Identifier.parse(combined);
	}

	public static Identifier vanilla(String path) {

		return Identifier.withDefaultNamespace(path);
	}
}