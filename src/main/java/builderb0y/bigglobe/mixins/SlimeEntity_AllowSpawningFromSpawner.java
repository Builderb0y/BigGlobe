package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

@Mixin(SlimeEntity.class)
public class SlimeEntity_AllowSpawningFromSpawner {

	@Inject(method = "canSpawn", at = @At(value = "INVOKE", target = "net/minecraft/world/WorldAccess.getBiome(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/registry/entry/RegistryEntry;"), cancellable = true)
	private static void bigglobe_allowSpawningFromSpawner(EntityType<SlimeEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> callback) {
		if (spawnReason == SpawnReason.SPAWNER) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}
}