package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.LevelAccessor;

@Mixin(Slime.class)
public class SlimeEntity_AllowSpawningFromSpawner {

	@Inject(method = "checkSlimeSpawnRules", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"), cancellable = true)
	private static void bigglobe_allowSpawningFromSpawner(EntityType<Slime> type, LevelAccessor world, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> callback) {
		if (spawnReason == EntitySpawnReason.SPAWNER) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}
}