package builderb0y.bigglobe.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.mixinInterfaces.MutableBlockEntityType;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

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
			if (!BlockStateVersions.isReplaceable(world.getBlockState(mutablePos))) return mutablePos;
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
			BigGlobeMod.LOGGER.warn("Expected " + clazz + " at " + pos + " in " + world + ", but got " + blockEntity + " instead.");
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
			RegistryKey<BlockEntityType<?>> id = RegistryVersions.blockEntityType().getKey(type).orElse(null);
			String name = id != null ? id.toString() : "(unregistered: " + type + " for block(s): " + ((MutableBlockEntityType)(type)).bigglobe_getBlocks() + ')';
			BigGlobeMod.LOGGER.warn("Expected " + name + " at " + pos + " in " + world + ", but got " + blockEntity + " instead.");
			return null;
		}
	}

	public static BlockBox chunkBox(ChunkPos pos, HeightLimitView height) {
		return new BlockBox(
			pos.getStartX(),
			height.getBottomY(),
			pos.getStartZ(),
			pos.getEndX(),
			height.getTopY() - 1,
			pos.getEndZ()
		);
	}

	public static BlockBox chunkBox(Chunk chunk) {
		return chunkBox(chunk.getPos(), chunk);
	}

	public static BlockBox surroundingChunkBox(ChunkPos pos, HeightLimitView height) {
		return new BlockBox(
			(pos.x - 1) << 4,
			height.getBottomY(),
			(pos.z - 1) << 4,
			((pos.x + 1) << 4) | 15,
			height.getTopY() - 1,
			((pos.z + 1) << 4) | 15
		);
	}

	public static BlockBox surroundingChunkBox(Chunk chunk) {
		return surroundingChunkBox(chunk.getPos(), chunk);
	}

	public static BlockBox createBlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		int tmp;
		if (maxX < minX) { tmp = maxX; maxX = minX; minX = tmp; }
		if (maxY < minY) { tmp = maxY; maxY = minY; minY = tmp; }
		if (maxZ < minZ) { tmp = maxZ; maxZ = minZ; minZ = tmp; }
		return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static BlockBox union(BlockBox box1, BlockBox box2) {
		return new BlockBox(
			Math.min(box1.getMinX(), box2.getMinX()),
			Math.min(box1.getMinY(), box2.getMinY()),
			Math.min(box1.getMinZ(), box2.getMinZ()),
			Math.max(box1.getMaxX(), box2.getMaxX()),
			Math.max(box1.getMaxY(), box2.getMaxY()),
			Math.max(box1.getMaxZ(), box2.getMaxZ())
		);
	}

	public static @Nullable BlockBox intersection(BlockBox box1, BlockBox box2) {
		int minX = Math.max(box1.getMinX(), box2.getMinX());
		int minY = Math.max(box1.getMinY(), box2.getMinY());
		int minZ = Math.max(box1.getMinZ(), box2.getMinZ());
		int maxX = Math.min(box1.getMaxX(), box2.getMaxX());
		int maxY = Math.min(box1.getMaxY(), box2.getMaxY());
		int maxZ = Math.min(box1.getMaxZ(), box2.getMaxZ());
		return (
			maxX >= minX && maxY >= minY && maxZ >= minZ
			? new BlockBox(minX, minY, minZ, maxX, maxY, maxZ)
			: null
		);
	}

	public static NbtIntArray blockBoxToNbt(BlockBox box) {
		return new NbtIntArray(new int[] { box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ() });
	}

	public static BlockBox blockBoxFromIntArray(int[] array) {
		if (array.length == 6) {
			return new BlockBox(array[0], array[1], array[2], array[3], array[4], array[5]);
		}
		else {
			throw new IllegalArgumentException("Serialized BlockBox is of wrong length: Expected 6, got " + array.length);
		}
	}

	public static boolean isReplaceableNonFluid(BlockState state) {
		return BlockStateVersions.isReplaceable(state) && state.getFluidState().isEmpty();
	}

	public static boolean isReplaceableNonFluid(BlockView world, BlockPos pos) {
		return isReplaceableNonFluid(world.getBlockState(pos));
	}
}