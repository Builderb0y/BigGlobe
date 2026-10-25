package builderb0y.bigglobe.versions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelData;

public class WorldPropertiesVersions {

	public static int getSpawnX(LevelData properties) {
		return properties.getSpawnPos().getX();
	}

	public static int getSpawnY(LevelData properties) {
		return properties.getSpawnPos().getY();
	}

	public static int getSpawnZ(LevelData properties) {
		return properties.getSpawnPos().getZ();
	}

	public static BlockPos getSpawnPos(LevelData properties) {
		return properties.getSpawnPos();
	}

	public static float getSpawnYaw(LevelData properties) {
		return properties.getSpawnAngle();
	}
}
