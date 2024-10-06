package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.block.EndPortalBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.versions.BlockStateVersions;

//used in 1.21+
@Mixin(EndPortalBlock.class)
public class EndPortalBlock_SpawnAtPreferredLocationInTheEnd {

	@WrapWithCondition(method = "createTeleportTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/EndPlatformFeature;generate(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/util/math/BlockPos;Z)V"))
	private boolean bigglobe_skipPlatformWhenRequested(ServerWorldAccess world, BlockPos pos, boolean breakBlocks) {
		return (
			!(((ServerWorld)(world)).getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) ||
			generator.end_overrides == null ||
			generator.end_overrides.spawning().obsidian_platform()
		);
	}

	@ModifyExpressionValue(method = "createTeleportTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;down()Lnet/minecraft/util/math/BlockPos;"))
	private BlockPos bigglobe_generatePlatformAtRequestedPosition(BlockPos original, @Local(ordinal = 1) ServerWorld destination, @Share("bigglobe_platformPosition") LocalRef<BlockPos> platformPosition) {
		int[] position;
		BlockPos result;
		if (destination.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			position = generator.end_overrides.spawning().location();
			BlockPos.Mutable pos = new BlockPos.Mutable(position[0], position[1], position[2]);
			Chunk chunk = destination.getChunk(pos);
			while (BlockStateVersions.isReplaceable(chunk.getBlockState(pos))) {
				pos.setY(pos.getY() - 1);
				if (pos.getY() < destination.getBottomY()) {
					platformPosition.set(result = new BlockPos(position[0], position[1], position[2]));
					return result;
				}
			}
			while (chunk.getBlockState(pos).isOpaqueFullCube(destination, pos)) {
				pos.setY(pos.getY() + 1);
			}
			pos.setY(pos.getY() + 1);
			platformPosition.set(result = pos.toImmutable());
			return result;
		}
		else {
			return original;
		}
	}

	@ModifyVariable(method = "createTeleportTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Direction;asRotation()F"))
	private Vec3d bigglobe_placePlayerAtRequestedPosition(Vec3d original, @Share("bigglobe_platformPosition") LocalRef<BlockPos> platformPosition) {
		BlockPos pos = platformPosition.get();
		if (pos != null) {
			return pos.toBottomCenterPos();
		}
		else {
			return original;
		}
	}
}