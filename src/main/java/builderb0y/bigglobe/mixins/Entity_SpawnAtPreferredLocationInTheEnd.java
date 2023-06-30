package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.chunkgen.BigGlobeEndChunkGenerator;

@Mixin(Entity.class)
public class Entity_SpawnAtPreferredLocationInTheEnd {

	@Redirect(method = "getTeleportTarget", at = @At(value = "FIELD", target = "Lnet/minecraft/server/world/ServerWorld;END_SPAWN_POS:Lnet/minecraft/util/math/BlockPos;"))
	private BlockPos bigglobe_spawnAtPreferredLocationInTheEnd(ServerWorld destination) {
		if (destination.getChunkManager().getChunkGenerator() instanceof BigGlobeEndChunkGenerator generator) {
			int[] position = generator.settings.nest().spawn_location();
			return new BlockPos(position[0], position[1], position[2]);
		}
		else {
			return ServerWorld.END_SPAWN_POS;
		}
	}
}