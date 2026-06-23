package builderb0y.bigglobe.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat;
import builderb0y.bigglobe.mixinInterfaces.MutableBlockEntityType;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

public class WorldUtil {

	/**
	sets a block state PROPERLY. there are some bugs in ChunkRegion where it doesn't
	do it right for states with block entities, and this method fixes that logic.
	*/
	public static void setBlockState(LevelAccessor world, BlockPos pos, BlockState state, int flags) {
		//ChunkRegion.setBlockState() doesn't call toImmutable(),
		//which could lead to block entities containing a leaked mutable pos.
		//note: calling toImmutable() is unnecessary if the state does not have a tile entity.
		boolean special = world instanceof WorldGenRegion && state.hasBlockEntity();
		if (special) pos = pos.immutable();
		world.setBlock(pos, state, flags);
		if (special) {
			//ChunkRegion's will prefer to create deferred block entities sometimes instead of regular block entities.
			//manually fetching the block entity will force it to convert from deferred to regular.
			BlockEntity blockEntity = getBlockEntity(world, pos, BlockEntity.class); //log error if nothing was found.
			//however, this does not remove it from the deferred list.
			ChunkAccess chunk = world.getChunk(pos);
			chunk.removeBlockEntity(pos);
			//NOW it's removed from the deferred list.
			//but it's also removed from the normal list,
			//so we need to re-add it.
			if (blockEntity != null) chunk.setBlockEntity(blockEntity);
		}
	}

	public static @Nullable MutableBlockPos findNonReplaceableGround(BlockGetter world, BlockPos start) {
		return findNonReplaceableGroundMutable(world, start.mutable());
	}

	public static @Nullable MutableBlockPos findNonReplaceableGroundMutable(BlockGetter world, MutableBlockPos mutablePos) {
		if (world instanceof LevelReader worldView) {
			world = worldView.getChunk(mutablePos);
		}
		while (true) {
			if (world.isOutsideBuildHeight(mutablePos)) return null;
			if (!BlockStateVersions.isReplaceable(world.getBlockState(mutablePos))) return mutablePos;
			mutablePos.setY(mutablePos.getY() - 1);
		}
	}

	@SuppressWarnings("unchecked")
	public static <B> @Nullable B getBlockEntity(BlockGetter world, BlockPos pos, Class<B> clazz) {
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
	public static <B extends BlockEntity> @Nullable B getBlockEntity(BlockGetter world, BlockPos pos, BlockEntityType<B> type) {
		if (DistantHorizonsCompat.isOnDistantHorizonThread()) return null;
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null && blockEntity.getType() == type) {
			return (B)(blockEntity);
		}
		else {
			ResourceKey<BlockEntityType<?>> id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getResourceKey(type).orElse(null);
			String name = id != null ? id.toString() : "(unregistered: " + type + " for block(s): " + ((MutableBlockEntityType)(type)).bigglobe_getBlocks() + ')';
			BigGlobeMod.LOGGER.warn("Expected " + name + " at " + pos + " in " + world + ", but got " + blockEntity + " instead.");
			return null;
		}
	}

	public static BoundingBox chunkBox(ChunkPos pos, LevelHeightAccessor height) {
		return chunkBox(
			pos,
			HeightLimitViewVersions.getMinY(height),
			HeightLimitViewVersions.getMaxY(height)
		);
	}

	public static BoundingBox chunkBox(ChunkPos pos, int minY, int maxY) {
		return new BoundingBox(
			pos.getMinBlockX(),
			minY,
			pos.getMinBlockZ(),
			pos.getMaxBlockX(),
			maxY - 1,
			pos.getMaxBlockZ()
		);
	}

	public static BoundingBox chunkBox(ChunkAccess chunk) {
		return chunkBox(chunk.getPos(), chunk);
	}

	public static BoundingBox surroundingChunkBox(ChunkPos pos, LevelHeightAccessor height) {
		return new BoundingBox(
			(pos.x() - 1) << 4,
			HeightLimitViewVersions.getMinY(height),
			(pos.z() - 1) << 4,
			((pos.x() + 1) << 4) | 15,
			HeightLimitViewVersions.getMaxY(height) - 1,
			((pos.z() + 1) << 4) | 15
		);
	}

	public static BoundingBox surroundingChunkBox(ChunkAccess chunk) {
		return surroundingChunkBox(chunk.getPos(), chunk);
	}

	public static BoundingBox createBlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		int tmp;
		if (maxX < minX) {
			tmp = maxX;
			maxX = minX;
			minX = tmp;
		}
		if (maxY < minY) {
			tmp = maxY;
			maxY = minY;
			minY = tmp;
		}
		if (maxZ < minZ) {
			tmp = maxZ;
			maxZ = minZ;
			minZ = tmp;
		}
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static BoundingBox union(BoundingBox box1, BoundingBox box2) {
		return new BoundingBox(
			Math.min(box1.minX(), box2.minX()),
			Math.min(box1.minY(), box2.minY()),
			Math.min(box1.minZ(), box2.minZ()),
			Math.max(box1.maxX(), box2.maxX()),
			Math.max(box1.maxY(), box2.maxY()),
			Math.max(box1.maxZ(), box2.maxZ())
		);
	}

	public static @Nullable BoundingBox intersection(BoundingBox box1, BoundingBox box2) {
		int minX = Math.max(box1.minX(), box2.minX());
		int minY = Math.max(box1.minY(), box2.minY());
		int minZ = Math.max(box1.minZ(), box2.minZ());
		int maxX = Math.min(box1.maxX(), box2.maxX());
		int maxY = Math.min(box1.maxY(), box2.maxY());
		int maxZ = Math.min(box1.maxZ(), box2.maxZ());
		return (
			maxX >= minX && maxY >= minY && maxZ >= minZ
			? new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
			: null
		);
	}

	public static IntArrayTag blockBoxToNbt(BoundingBox box) {
		return new IntArrayTag(new int[] { box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() });
	}

	public static BoundingBox blockBoxFromIntArray(int[] array) {
		if (array.length == 6) {
			return new BoundingBox(array[0], array[1], array[2], array[3], array[4], array[5]);
		}
		else {
			throw new IllegalArgumentException("Serialized BlockBox is of wrong length: Expected 6, got " + array.length);
		}
	}

	public static boolean isReplaceableNonFluid(BlockState state) {
		return BlockStateVersions.isReplaceable(state) && state.getFluidState().isEmpty();
	}

	public static boolean isReplaceableNonFluid(BlockGetter world, BlockPos pos) {
		return isReplaceableNonFluid(world.getBlockState(pos));
	}

	/**
	vanilla logic considers a chunk to be "loaded" when it's
	close enough to a player that it "should" be loaded,
	even if it hasn't been generated yet.
	*/
	public static boolean isAreaLoaded(ServerLevel world, BoundingBox box) {
		int
			minX = box.minX() >> 4,
			minZ = box.minZ() >> 4,
			maxX = box.maxX() >> 4,
			maxZ = box.maxZ() >> 4;
		for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
			for (int chunkX = minX; chunkX <= maxX; chunkX++) {
				if (world.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
					return false;
				}
			}
		}
		return true;
	}
}