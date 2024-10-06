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
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
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
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			ScriptedColumn column = generator.newColumn(world, 0, 0, Purpose.GENERIC);
			Vector2d direction = new Vector2d(gatewayPos.getX(), gatewayPos.getZ()).normalize();
			Vector2d position = new Vector2d();
			double minRadius = generator.end_overrides.outer_gateways().min_radius();
			double maxRadius = generator.end_overrides.outer_gateways().max_radius();
			double step = generator.end_overrides.outer_gateways().step();
			ColumnToBooleanScript condition = generator.end_overrides.outer_gateways().condition();
			for (double radius = minRadius; radius <= maxRadius; radius += step) {
				position.set(direction).mul(radius);
				column.setParamsUnchecked(column.params.at(BigGlobeMath.floorI(position.x), BigGlobeMath.floorI(position.y)));
				if (condition.get(column)) {
					bigglobe_exitPosition = position;
					callback.setReturnValue(
						new Vec3d(
							position.x,
							generator.getHeightOnGround(
								BigGlobeMath.floorI(position.x),
								BigGlobeMath.floorI(position.y),
								Heightmap.Type.WORLD_SURFACE_WG,
								world,
								world.getChunkManager().getNoiseConfig()
							),
							position.y
						)
					);
					return;
				}
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
			if (world.getChunkManager() instanceof ServerChunkManager manager && manager.getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
				BlockPos.Mutable mutablePos = new BlockPos.Mutable();
				for (
					GoldenSpiralIterator iterator = new GoldenSpiralIterator(basePosition.x, basePosition.y, 2.0D, world.random.nextLong());
					iterator.radius <= 64.0D;
					iterator.next()
				) {
					int topY = generator.getHeight(iterator.floorX(), iterator.floorY(), Heightmap.Type.WORLD_SURFACE_WG, world, manager.getNoiseConfig());
					if (topY > world.getBottomY()) {
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
		if (world instanceof ServerWorld serverWorld && serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			BlockPos.Mutable
				search = new BlockPos.Mutable(),
				found  = pos.mutableCopy().setY(world.getBottomY());
			for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
				innerSquare:
				for (int offsetZ = -searchRadius; offsetZ <= searchRadius; offsetZ++) {
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