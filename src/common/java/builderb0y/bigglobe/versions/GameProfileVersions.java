package builderb0y.bigglobe.versions;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

public class GameProfileVersions {

	public static UUID getUUID(GameProfile gameProfile) {

		return gameProfile.id();
	}
}