package builderb0y.bigglobe.versions;

import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;

public class SpawnEntryVersions {

	public static EntityType<?> type(SpawnEntry entry) {
		return entry.type #if MC_VERSION >= MC_1_21_5 () #endif;
	}

	public static int minCount(SpawnEntry entry) {
		return entry.minGroupSize #if MC_VERSION >= MC_1_21_5 () #endif;
	}

	public static int maxCount(SpawnEntry entry) {
		return entry.maxGroupSize #if MC_VERSION >= MC_1_21_5 () #endif;
	}
}