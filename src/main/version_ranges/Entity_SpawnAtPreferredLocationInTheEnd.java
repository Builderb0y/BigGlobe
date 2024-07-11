package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.versions.BlockStateVersions;

//used in [1.20.1, 1.21).
@Mixin(Entity.class)
public class Entity_SpawnAtPreferredLocationInTheEnd {

	@Redirect(method = "getTeleportTarget", at = @At(value = "FIELD", target = "Lnet/minecraft/server/world/ServerWorld;END_SPAWN_POS:Lnet/minecraft/util/math/BlockPos;"))
	private BlockPos bigglobe_spawnAtPreferredLocationInTheEnd(ServerWorld destination) {
		if (destination.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			int[] position = generator.end_overrides.spawning().location();
			BlockPos.Mutable pos = new BlockPos.Mutable(position[0], position[1], position[2]);
			Chunk chunk = destination.getChunk(pos);
			while (BlockStateVersions.isReplaceable(chunk.getBlockState(pos))) {
				pos.setY(pos.getY() - 1);
				if (pos.getY() < destination.getBottomY()) {
					return new BlockPos(position[0], position[1], position[2]);
				}
			}
			while (chunk.getBlockState(pos).isOpaqueFullCube(destination, pos)) {
				pos.setY(pos.getY() + 1);
			}
			pos.setY(pos.getY() + 1);
			return pos.toImmutable();
		}
		else {
			return ServerWorld.END_SPAWN_POS;
		}
	}
}