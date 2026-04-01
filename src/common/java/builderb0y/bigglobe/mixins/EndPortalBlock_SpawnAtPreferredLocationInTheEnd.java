package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

@Mixin(EndPortalBlock.class)
public class EndPortalBlock_SpawnAtPreferredLocationInTheEnd {

	@WrapWithCondition(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/EndPlatformFeature;createEndPlatform(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Z)V"))
	private boolean bigglobe_skipPlatformWhenRequested(ServerLevelAccessor world, BlockPos pos, boolean breakBlocks) {
		return (
			!(((ServerLevel)(world)).getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) ||
			generator.game_mechanics.end() == null ||
			generator.game_mechanics.end().spawning().obsidian_platform()
		);
	}

	@ModifyExpressionValue(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"))
	private BlockPos bigglobe_generatePlatformAtRequestedPosition(BlockPos original, @Local(ordinal = 1) ServerLevel destination, @Share("bigglobe_platformPosition") LocalRef<BlockPos> platformPosition) {
		int[] position;
		BlockPos result;
		if (destination.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
			position = generator.game_mechanics.end().spawning().location();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(position[0], position[1], position[2]);
			ChunkAccess chunk = destination.getChunk(pos);
			while (BlockStateVersions.isReplaceable(chunk.getBlockState(pos))) {
				pos.setY(pos.getY() - 1);
				if (pos.getY() < HeightLimitViewVersions.getMinY(destination)) {
					platformPosition.set(result = new BlockPos(position[0], position[1], position[2]));
					return result;
				}
			}
			while (BlockStateVersions.isOpaqueFullCube(chunk.getBlockState(pos), destination, pos)) {
				pos.setY(pos.getY() + 1);
			}
			pos.setY(pos.getY() + 1);
			platformPosition.set(result = pos.immutable());
			return result;
		}
		else {
			return original;
		}
	}

	@ModifyVariable(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;toYRot()F"))
	private Vec3 bigglobe_placePlayerAtRequestedPosition(Vec3 original, @Share("bigglobe_platformPosition") LocalRef<BlockPos> platformPosition) {
		BlockPos pos = platformPosition.get();
		if (pos != null) {
			return pos.getBottomCenter();
		}
		else {
			return original;
		}
	}
}