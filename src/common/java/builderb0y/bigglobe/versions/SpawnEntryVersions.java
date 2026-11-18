package builderb0y.bigglobe.versions;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

public class SpawnEntryVersions {

	public static EntityType<?> type(SpawnerData entry) {
		return entry.type();
	}

	public static int minCount(SpawnerData entry) {
		#if MC_VERSION >= MC_26_3_0
			return entry.count().minInclusive();
		#else
			return entry.minCount();
		#endif
	}

	public static int maxCount(SpawnerData entry) {
		#if MC_VERSION >= MC_26_3_0
			return entry.count().maxInclusive();
		#else
			return entry.maxCount();
		#endif
	}
}