package builderb0y.bigglobe.versions;

import net.minecraft.util.Identifier;

public class IdentifierVersions {

	public static Identifier create(String namespace, String path) {
		#if MC_VERSION >= MC_1_21_0
			return Identifier.of(namespace, path);
		#else
			return new Identifier(namespace, path);
		#endif
	}

	public static Identifier create(String combined) {
		#if MC_VERSION >= MC_1_21_0
			return Identifier.of(combined);
		#else
			return new Identifier(combined);
		#endif
	}

	public static Identifier vanilla(String path) {
		#if MC_VERSION >= MC_1_21_0
			return Identifier.ofVanilla(path);
		#else
			return new Identifier("minecraft", path);
		#endif
	}
}