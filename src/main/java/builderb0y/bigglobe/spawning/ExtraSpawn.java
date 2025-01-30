package builderb0y.bigglobe.spawning;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.util.DelayedEntryList;

public record ExtraSpawn(
	DelayedEntryList<Biome> biomes,
	RegistryEntry<EntityType<?>> type,
	int weight,
	@VerifyIntRange(min = 1) int minCount,
	@VerifySorted(greaterThanOrEqual = "minCount") int maxCount
) {

	public SpawnEntry toEntry() {
		return new SpawnEntry(this.type.value(), this.weight, this.minCount, this.maxCount);
	}
}