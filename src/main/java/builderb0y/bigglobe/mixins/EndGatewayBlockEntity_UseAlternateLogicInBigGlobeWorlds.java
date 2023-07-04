package builderb0y.bigglobe.mixins;

import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
	private static void bigglobe_useColumnMaxYForOutwardSearch(ServerWorld world, BlockPos gatewayPos, CallbackInfoReturnable<Vec3d> callback) {
		bigglobe_exitPosition = null;
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeEndChunkGenerator generator) {
			EndColumn column = generator.column(0, 0);
			Vector2d position = new Vector2d(gatewayPos.getX(), gatewayPos.getZ()).normalize(16.0D);
			Vector2d step = new Vector2d(position);
			position.mul(world.random.nextDouble());
			for (int attempt = 1; attempt <= 2048 / 16; attempt++) {
				column.setPosUnchecked(BigGlobeMath.floorI(position.x), BigGlobeMath.floorI(position.y));
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
	private static void bigglobe_useColumnMaxYForPositionInsideChunk(WorldChunk chunk, CallbackInfoReturnable<BlockPos> callback) {
		World world = chunk.getWorld();
		Vector2d basePosition = bigglobe_exitPosition;
		if (basePosition != null) {
			bigglobe_exitPosition = null;
			if (world.getChunkManager() instanceof ServerChunkManager manager && manager.getChunkGenerator() instanceof BigGlobeEndChunkGenerator generator) {
				EndColumn column = generator.column(0, 0);
				GoldenSpiralIterator iterator = new GoldenSpiralIterator(basePosition.x, basePosition.y, 2.0D, world.random.nextLong());
				BlockPos.Mutable mutablePos = new BlockPos.Mutable();
				for (int attempt = 0; attempt < 32; attempt++) {
					column.setPosUnchecked(iterator.floorX(), iterator.floorY());
					if (column.hasTerrain()) {
						int topY = column.getFinalTopHeightI();
						mutablePos.set(iterator.floorX(), topY, iterator.floorY());
						Chunk newChunk = world.getChunk(mutablePos);
						if (
							newChunk.getBlockState(mutablePos.setY(topY - 1)).isOpaqueFullCube(world, mutablePos) &&
							newChunk.getBlockState(mutablePos.setY(topY    )).isAir() &&
							newChunk.getBlockState(mutablePos.setY(topY + 1)).isAir()
						) {
							callback.setReturnValue(mutablePos.setY(topY - 1).toImmutable());
							return;
						}
					}
					iterator.next();
				}
				callback.setReturnValue(null);
				return;
			}
		}
	}

	@Inject(method = "findExitPortalPos", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_useColumnMaxYForHighestYLevelSearch(BlockView world, BlockPos pos, int searchRadius, boolean force, CallbackInfoReturnable<BlockPos> callback) {
		if (world instanceof ServerWorld serverWorld && serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeEndChunkGenerator generator) {
			BlockPos.Mutable
				search = new BlockPos.Mutable(),
				found  = pos.mutableCopy().setY(Integer.MIN_VALUE);
			EndColumn column = generator.column(0, 0);
			for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
				innerSquare: for (int offsetZ = -searchRadius; offsetZ <= searchRadius; offsetZ++) {
					column.setPosUnchecked(pos.getX() + offsetX, pos.getZ() + offsetZ);
					search.set(column.x, column.getFinalTopHeightI(), column.z);
					int minY = column.getFinalBottomHeightI();
					while (bigglobe_canSpawnAt(world, search)) {
						search.setY(search.getY() + 1);
					}
					while (!bigglobe_canSpawnAt(world, search)) {
						search.setY(search.getY() - 1);
						if (search.getY() < minY) continue innerSquare;
					}
					if (search.getY() > found.getY()) {
						found.set(search);
					}
				}
			}
			callback.setReturnValue(found.toImmutable());
		}
	}

	private static boolean bigglobe_canSpawnAt(BlockView world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isFullCube(world, pos) && !state.isOf(Blocks.BEDROCK);
	}
}