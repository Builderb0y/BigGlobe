package builderb0y.bigglobe.versions;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

public class GameProfileVersions {

	public static UUID getUUID(GameProfile gameProfile) {
		#if MC_VERSION >= MC_1_21_9
			return gameProfile.id();
		#else
			return gameProfile.getId();
		#endif
	}
}