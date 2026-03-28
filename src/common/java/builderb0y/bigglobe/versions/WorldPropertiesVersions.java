package builderb0y.bigglobe.versions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelData;

public class WorldPropertiesVersions {

	public static int getSpawnX(LevelData properties) {

		return properties.getRespawnData().pos().getX();
	}

	public static int getSpawnY(LevelData properties) {

		return properties.getRespawnData().pos().getY();
	}

	public static int getSpawnZ(LevelData properties) {

		return properties.getRespawnData().pos().getZ();
	}

	public static BlockPos getSpawnPos(LevelData properties) {

		return properties.getRespawnData().pos();
	}

	public static float getSpawnYaw(LevelData properties) {

		return properties.getRespawnData().yaw();
	}
}