package builderb0y.bigglobe.mixins;

import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.bigglobe.chunkgen.BigGlobeEndChunkGenerator;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.GoldenSpiralIterator;

@Mixin(EndGatewayBlockEntity.class)
public class EndGatewayBlockEntity_UseAlternateLogicInBigGlobeWorlds {

	@Unique
	private static Vector2d bigglobe_exitPosition;

	@Inject(method = "findTeleportLocation", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_useColumnMaxYForSearch(ServerWorld world, BlockPos gatewayPos, CallbackInfoReturnable<Vec3d> callback) {
		bigglobe_exitPosition = null;
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeEndChunkGenerator generator) {
			EndColumn column = generator.column(0, 0);
			Vector2d position = new Vector2d(gatewayPos.getX(), gatewayPos.getZ()).normalize(16.0D);
			Vector2d step = new Vector2d(position);
			position.mul(world.random.nextDouble());
			for (int attempt = 1; attempt <= 2048 / 16; attempt++) {
				column.setPosUnchecked(BigGlobeMath.floorI(position.x), BigGlobeMath.floorI(position.y));
				generator.populateHeightEmpty(column);
				if (column.hasTerrain()) {
					bigglobe_exitPosition = position;
					callback.setReturnValue(new Vec3d(position.x, column.getFinalTopHeightD(), position.y));
					return;
				}
				position.add(step);
			}
		}
	}

	@Inject(method = "findPortalPosition", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_useColumnMaxYForRefinement(WorldChunk chunk, CallbackInfoReturnable<BlockPos> callback) {
		World world = chunk.getWorld();
		Vector2d basePosition = bigglobe_exitPosition;
		if (basePosition != null) {
			bigglobe_exitPosition = null;
			if (world.getChunkManager() instanceof ServerChunkManager manager && manager.getChunkGenerator() instanceof BigGlobeEndChunkGenerator generator) {
				EndColumn column = generator.column(0, 0);
				GoldenSpiralIterator iterator = new GoldenSpiralIterator(basePosition.x, basePosition.y, 2.0D, world.random.nextLong());
				BlockPos.Mutable mutablePos = new BlockPos.Mutable();
				for (int attempt = 0; attempt < 32; attempt++) {
					iterator.next();
					column.setPosUnchecked(iterator.floorX(), iterator.floorY());
					generator.populateHeightEmpty(column);
					if (column.hasTerrain()) {
						mutablePos.set(iterator.floorX(), column.getFinalTopHeightD(), iterator.floorY());
						Chunk newChunk = world.getChunk(mutablePos);
						if (
							newChunk.getBlockState(mutablePos.setY(column.getFinalTopHeightI() - 1)).isOpaqueFullCube(world, mutablePos) &&
							newChunk.getBlockState(mutablePos.setY(mutablePos.getY() + 1)).isAir() &&
							newChunk.getBlockState(mutablePos.setY(mutablePos.getY() + 1)).isAir()
						) {
							callback.setReturnValue(mutablePos.setY(mutablePos.getY() - 2).toImmutable());
							return;
						}
					}
				}
				callback.setReturnValue(null);
				return;
			}
		}
	}

	@Redirect(method = "findExitPortalPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BlockView;getTopY()I"))
	private static int bigglobe_startAtGatewayYLevel(BlockView world, BlockView param1, BlockPos pos) {
		if (world instanceof ServerWorld serverWorld && serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeEndChunkGenerator) {
			return pos.getY();
		}
		else {
			return world.getTopY();
		}
	}
}