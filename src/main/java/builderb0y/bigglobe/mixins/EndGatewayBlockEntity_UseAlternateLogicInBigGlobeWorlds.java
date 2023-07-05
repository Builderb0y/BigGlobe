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

/** vanilla end gateway logic is a mess. */
@Mixin(EndGatewayBlockEntity.class)
public class EndGatewayBlockEntity_UseAlternateLogicInBigGlobeWorlds {

	@Unique
	private static Vector2d bigglobe_exitPosition;

	/**
	the first thing a gateway will do if it has no target
	is to search outwards until it finds terrain.
	this is a problem for me, because it uses the
	highest non-empty chunk section as a starting point,
	which means it could detect ring and bridge clouds, which is undesirable.
	*/
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

	/**
	after an end gateway has searched outwards until it finds terrain,
	it will then try to find... something... within the chunk it lands in.
	honestly, I can't follow this logic easily. but what I can tell is that whatever
	it's looking for, it ranges between Y 30 and the highest non-empty chunk section,
	meaning that it still has a chance of not detecting big globe terrain properly,
	since my mountain terrain can be below Y 30.
	it is also hard-coded to only detect end stone,
	not my end nylium or overgrown end stone.

	this method fixes both of these issues too.
	my logic for what I'm looking for is simply a place nearby
	the found terrain where a player can safely be teleported to.
	*/
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

	/**
	after an end gateway has found what it's looking for in a chunk,
	it tries to find the block with the highest Y level within 16 blocks of the found location.
	as you can probably guess, this detects ring and bridge clouds too,
	and I don't want gateways to place the player on clouds UNLESS there is no
	other viable location for them to be teleported to, so I tweak this method too.

	oh and btw, this method gets called twice. once to place the return gateway,
	and once to position the player around the return gateway.
	but positioning the player only checks a 5 block radius instead of a 16 block radius.
	*/
	@Inject(method = "findExitPortalPos", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_useAlternateLogicForHighestYLevelSearch(BlockView world, BlockPos pos, int searchRadius, boolean force, CallbackInfoReturnable<BlockPos> callback) {
		if (world instanceof ServerWorld serverWorld && serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeEndChunkGenerator) {
			BlockPos.Mutable
				search = new BlockPos.Mutable(),
				found  = pos.mutableCopy().setY(world.getBottomY());
			for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
				innerSquare: for (int offsetZ = -searchRadius; offsetZ <= searchRadius; offsetZ++) {
					search.set(pos.getX() + offsetX, pos.getY(), pos.getZ() + offsetZ);
					while (bigglobe_canSpawnOn(world, search)) {
						search.setY(search.getY() + 1);
					}
					while (!bigglobe_canSpawnOn(world, search)) {
						search.setY(search.getY() - 1);
						if (search.getY() < found.getY()) continue innerSquare;
					}
					found.set(search);
				}
			}
			if (found.getY() == world.getBottomY()) found.set(pos);
			callback.setReturnValue(found.toImmutable());
		}
	}

	private static boolean bigglobe_canSpawnOn(BlockView world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isFullCube(world, pos) && !state.isOf(Blocks.BEDROCK);
	}
}