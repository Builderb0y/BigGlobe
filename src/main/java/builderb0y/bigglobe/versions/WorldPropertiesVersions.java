package builderb0y.bigglobe.versions;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldProperties;

public class WorldPropertiesVersions {

	public static int getSpawnX(WorldProperties properties) {
		#if MC_VERSION >= MC_1_20_5
			return properties.getSpawnPos().getX();
		#else
			return properties.getSpawnX();
		#endif
	}

	public static int getSpawnY(WorldProperties properties) {
		#if MC_VERSION >= MC_1_20_5
			return properties.getSpawnPos().getY();
		#else
			return properties.getSpawnY();
		#endif
	}

	public static int getSpawnZ(WorldProperties properties) {
		#if MC_VERSION >= MC_1_20_5
			return properties.getSpawnPos().getZ();
		#else
			return properties.getSpawnZ();
		#endif
	}

	public static BlockPos getSpawnPos(WorldProperties properties) {
		#if MC_VERSION >= MC_1_20_5
			return properties.getSpawnPos();
		#else
			return new BlockPos(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ());
		#endif
	}
}