package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.chunkgen.BigGlobeEndChunkGenerator;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntity_CreateEndSpawnPlatformOnlyIfPreferred {

	@Inject(method = "createEndSpawnPlatform", at = @At("HEAD"), cancellable = true)
	private void bigglobe_dontSpawnEndPlatformUnlessDesired(ServerWorld world, BlockPos centerPos, CallbackInfo callback) {
		BlockPos downPos;
		if (
			world.getChunkManager().getChunkGenerator() instanceof BigGlobeEndChunkGenerator generator &&
			!generator.settings.nest.spawn_obsidian_platform() &&
			world.getBlockState(downPos = centerPos.down()).isOpaqueFullCube(world, downPos)
		) {
			callback.cancel();
		}
	}
}