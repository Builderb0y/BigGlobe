package builderb0y.bigglobe.versions;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

public class SpawnEntryVersions {

	public static EntityType<?> type(SpawnerData entry) {
		return entry.type();
	}

	public static int minCount(SpawnerData entry) {
		return entry.minCount();
	}

	public static int maxCount(SpawnerData entry) {
		return entry.maxCount();
	}
}