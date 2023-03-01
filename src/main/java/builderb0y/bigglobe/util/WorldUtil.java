package builderb0y.bigglobe.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;

public class WorldUtil {

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
			Identifier id = Registry.BLOCK_ENTITY_TYPE.getId(type);
			//todo: add valid blocks to message if/when I add an access widener for that.
			String name = id != null ? id.toString() : "(unregistered: " + type + ')';
			BigGlobeMod.LOGGER.warn("Expected " + name + " at " + pos + " in " + world + ", but got "+ blockEntity + " instead.");
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