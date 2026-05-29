package builderb0y.bigglobe.mixins;

import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted2.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.GoldenSpiralIterator;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
vanilla end gateway logic is a mess.
*/
@Mixin(TheEndGatewayBlockEntity.class)
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
	@Inject(method = "findExitPortalXZPosTentative", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_useColumnMaxYForOutwardSearch(ServerLevel world, BlockPos gatewayPos, CallbackInfoReturnable<Vec3> callback) {
		bigglobe_exitPosition = null;
		if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
			ScriptedColumn column = generator.newColumn(world, 0, 0, ColumnUsage.GENERIC.normalHints());
			Vector2d direction = new Vector2d(gatewayPos.getX(), gatewayPos.getZ()).normalize();
			Vector2d position = new Vector2d();
			double minRadius = generator.game_mechanics.end().outer_gateways().min_radius();
			double maxRadius = generator.game_mechanics.end().outer_gateways().max_radius();
			double step = generator.game_mechanics.end().outer_gateways().step();
			ColumnToBooleanScript condition = generator.game_mechanics.end().outer_gateways().condition();
			for (double radius = minRadius; radius <= maxRadius; radius += step) {
				position.set(direction).mul(radius);
				column.setParamsUnchecked(column.params.at(BigGlobeMath.floorI(position.x), BigGlobeMath.floorI(position.y)));
				if (condition.get(column)) {
					bigglobe_exitPosition = position;
					callback.setReturnValue(
						new Vec3(
							position.x,
							generator.getFirstFreeHeight(
								BigGlobeMath.floorI(position.x),
								BigGlobeMath.floorI(position.y),
								Heightmap.Types.WORLD_SURFACE_WG,
								world,
								world.getChunkSource().randomState()
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
	@Inject(method = "findValidSpawnInChunk", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_useColumnMaxYForPositionInsideChunk(LevelChunk chunk, CallbackInfoReturnable<BlockPos> callback) {
		Level world = chunk.getLevel();
		Vector2d basePosition = bigglobe_exitPosition;
		if (basePosition != null) {
			bigglobe_exitPosition = null;
			if (world.getChunkSource() instanceof ServerChunkCache manager && manager.getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
				BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
				for (
					GoldenSpiralIterator iterator = new GoldenSpiralIterator(basePosition.x, basePosition.y, 2.0D, world.getRandom().nextLong());
					iterator.radius <= 64.0D;
					iterator.next()
				) {
					int topY = generator.getBaseHeight(iterator.floorX(), iterator.floorY(), Heightmap.Types.WORLD_SURFACE_WG, world, manager.randomState());
					if (topY > HeightLimitViewVersions.getMinY(world)) {
						mutablePos.set(iterator.floorX(), topY, iterator.floorY());
						ChunkAccess newChunk = world.getChunk(mutablePos);
						if (
							BlockStateVersions.isOpaqueFullCube(
								newChunk.getBlockState(mutablePos.setY(topY - 1)),
								world,
								mutablePos
							) &&
							newChunk.getBlockState(mutablePos.setY(topY)).isAir() &&
							newChunk.getBlockState(mutablePos.setY(topY + 1)).isAir()
						) {
							callback.setReturnValue(mutablePos.setY(topY - 1).immutable());
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
	@Inject(method = "findTallestBlock", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_useAlternateLogicForHighestYLevelSearch(BlockGetter world, BlockPos pos, int searchRadius, boolean force, CallbackInfoReturnable<BlockPos> callback) {
		if (world instanceof ServerLevel serverWorld && serverWorld.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
			BlockPos.MutableBlockPos
				search = new BlockPos.MutableBlockPos(),
				found = pos.mutable().setY(HeightLimitViewVersions.getMinY(world));
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
			if (found.getY() == HeightLimitViewVersions.getMinY(world)) found.set(pos);
			callback.setReturnValue(found.immutable());
		}
	}

	private static boolean bigglobe_canSpawnOn(BlockGetter world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isCollisionShapeFullBlock(world, pos) && !state.is(Blocks.BEDROCK);
	}
}