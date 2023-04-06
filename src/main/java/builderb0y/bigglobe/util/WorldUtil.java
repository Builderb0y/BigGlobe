package builderb0y.bigglobe.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;

public class WorldUtil {

	/**
	sets a block state PROPERLY. there are some bugs in ChunkRegion where it doesn't
	do it right for states with block entities, and this method fixes that logic.
	*/
	public static void setBlockState(WorldAccess world, BlockPos pos, BlockState state, int flags) {
		//ChunkRegion.setBlockState() doesn't call toImmutable(),
		//which could lead to block entities containing a leaked mutable pos.
		//note: calling toImmutable() is unnecessary if the state does not have a tile entity.
		boolean special = world instanceof ChunkRegion && state.hasBlockEntity();
		if (special) pos = pos.toImmutable();
		world.setBlockState(pos, state, flags);
		if (special) {
			//ChunkRegion's will prefer to create deferred block entities sometimes instead of regular block entities.
			//manually fetching the block entity will force it to convert from deferred to regular.
			BlockEntity blockEntity = getBlockEntity(world, pos, BlockEntity.class); //log error if nothing was found.
			//however, this does not remove it from the deferred list.
			Chunk chunk = world.getChunk(pos);
			chunk.removeBlockEntity(pos);
			//NOW it's removed from the deferred list.
			//but it's also removed from the normal list,
			//so we need to re-add it.
			if (blockEntity != null) chunk.setBlockEntity(blockEntity);
		}
	}

	public static BlockPos.@Nullable Mutable findNonReplaceableGround(BlockView world, BlockPos start) {
		return findNonReplaceableGroundMutable(world, start.mutableCopy());
	}

	public static BlockPos.@Nullable Mutable findNonReplaceableGroundMutable(BlockView world, BlockPos.Mutable mutablePos) {
		if (world instanceof WorldView worldView) {
			world = worldView.getChunk(mutablePos);
		}
		while (true) {
			if (world.isOutOfHeightLimit(mutablePos)) return null;
			if (!world.getBlockState(mutablePos).getMaterial().isReplaceable()) return mutablePos;
			mutablePos.setY(mutablePos.getY() - 1);
		}
	}

	@SuppressWarnings("unchecked")
	public static <B> @Nullable B getBlockEntity(BlockView world, BlockPos pos, Class<B> clazz) {
		if (DistantHorizonsCompat.isOnDistantHorizonThread()) return null;
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (clazz.isInstance(blockEntity)) {
			return (B)(blockEntity);
		}
		else {
			BigGlobeMod.LOGGER.warn("Expected " + clazz.getTypeName() + " at " + pos + " in " + world + ", but got " + blockEntity + " instead.");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <B extends BlockEntity> @Nullable B getBlockEntity(BlockView world, BlockPos pos, BlockEntityType<B> type) {
		if (DistantHorizonsCompat.isOnDistantHorizonThread()) return null;
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null && blockEntity.getType() == type) {
			return (B)(blockEntity);
		}
		else {
			Identifier id = Registries.BLOCK_ENTITY_TYPE.getId(type);
			//todo: add valid blocks to message if/when I add an access widener for that.
			String name = id != null ? id.toString() : "(unregistered: " + type + ')';
			BigGlobeMod.LOGGER.warn("Expected " + name + " at " + pos + " in " + world + ", but got " + blockEntity + " instead.");
			return null;
		}
	}

	public static BlockBox chunkBox(Chunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		return new BlockBox(
			chunkPos.getStartX(),
			chunk.getBottomY(),
			chunkPos.getStartZ(),
			chunkPos.getEndX(),
			chunk.getTopY() - 1,
			chunkPos.getEndZ()
		);
	}

	public static BlockBox createBlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		int tmp;
		if (maxX < minX) { tmp = maxX; maxX = minX; minX = tmp; }
		if (maxY < minY) { tmp = maxY; maxY = minY; minY = tmp; }
		if (maxZ < minZ) { tmp = maxZ; maxZ = minZ; minZ = tmp; }
		return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static boolean isReplaceableNonFluid(BlockState state) {
		return state.getMaterial().isReplaceable() && state.getFluidState().isEmpty();
	}

	public static boolean isReplaceableNonFluid(BlockView world, BlockPos pos) {
		return isReplaceableNonFluid(world.getBlockState(pos));
	}
}