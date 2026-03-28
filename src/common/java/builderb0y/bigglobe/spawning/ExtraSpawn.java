package builderb0y.bigglobe.spawning;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.util.DelayedEntryList;
import net.minecraft.core.Holder;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

public record ExtraSpawn(
	DelayedEntryList<Biome> biomes,
	Holder<EntityType<?>> type,
	@VerifyIntRange(min = 0L) int weight,
	@VerifyIntRange(min = 1L) int minCount,
	@VerifySorted(greaterThanOrEqual = "minCount") int maxCount
) {

	public Weighted<SpawnerData> toEntry() {
		return new Weighted<>(new SpawnerData(this.type.value(), this.minCount, this.maxCount), this.weight);
	}
}