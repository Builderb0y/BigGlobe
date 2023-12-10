package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


#if MC_VERSION >= MC_1_20_3
	import net.minecraft.block.spawner.MobSpawnerLogic;
#else
	import net.minecraft.world.MobSpawnerLogic;
#endif

@Mixin(MobSpawnerLogic.class)
public interface MobSpawnerLogic_GettersAndSettersForEverything {

	@Accessor("spawnDelay")
	public abstract int bigglobe_getSpawnDelay();

	@Accessor("spawnDelay")
	public abstract void bigglobe_setSpawnDelay(int spawnDelay);

	@Accessor("minSpawnDelay")
	public abstract int bigglobe_getMinSpawnDelay();

	@Accessor("minSpawnDelay")
	public abstract void bigglobe_setMinSpawnDelay(int minSpawnDelay);

	@Accessor("maxSpawnDelay")
	public abstract int bigglobe_getMaxSpawnDelay();

	@Accessor("maxSpawnDelay")
	public abstract void bigglobe_setMaxSpawnDelay(int maxSpawnDelay);

	@Accessor("spawnCount")
	public abstract int bigglobe_getSpawnCount();

	@Accessor("spawnCount")
	public abstract void bigglobe_setSpawnCount(int spawnCount);

	@Accessor("maxNearbyEntities")
	public abstract int bigglobe_getMaxNearbyEntities();

	@Accessor("maxNearbyEntities")
	public abstract void bigglobe_setMaxNearbyEntities(int maxNearbyEntities);

	@Accessor("requiredPlayerRange")
	public abstract int bigglobe_getRequiredPlayerRange();

	@Accessor("requiredPlayerRange")
	public abstract void bigglobe_setRequiredPlayerRange(int requiredPlayerRange);

	@Accessor("spawnRange")
	public abstract int bigglobe_getSpawnRange();

	@Accessor("spawnRange")
	public abstract void bigglobe_setSpawnRange(int spawnRange);
}