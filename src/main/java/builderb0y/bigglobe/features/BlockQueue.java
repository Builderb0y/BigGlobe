package builderb0y.bigglobe.features;

import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;

import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.bigglobe.blocks.BlockStates;

/**
used by features which cannot compute their own space requirements with certainty in advance.
in other words, the space requirements can only be checked while generating the feature.
features like this place blocks into a BlockQueue instead of directly into the world,
and the space requirements are checked for each block added to the queue.
if the space requirements are met for all the blocks once the feature is done generating,
then the queue is copied to the world. otherwise, the queue is discarded and nothing happens.
*/
@Debug.Renderer(hasChildren = "true", childrenArray = "this.intellij_childrenArray()")
public class BlockQueue {

	public Long2ObjectLinkedOpenHashMap<BlockState> queuedBlocks = new Long2ObjectLinkedOpenHashMap<>(1024);
	public Long2ObjectLinkedOpenHashMap<BlockEntity> queuedBlockEntities = new Long2ObjectLinkedOpenHashMap<>(0);
	public int flags;

	public BlockQueue(int flags) {
		this.flags = flags;
	}

	public BlockQueue(boolean causeBlockUpdates) {
		this.flags = (
			causeBlockUpdates
			? Block.NOTIFY_ALL
			: Block.NOTIFY_LISTENERS | Block.FORCE_STATE
		);
	}

	public BlockQueueStructureWorldAccess createWorld(StructureWorldAccess world) {
		return new BlockQueueStructureWorldAccess(world, this);
	}

	public void queueBlock(BlockPos pos, BlockState state) {
		this.queueBlock(pos.asLong(), state);
	}

	public void queueBlock(long pos, BlockState state) {
		this.queuedBlocks.put(pos, state);
		if (state.hasBlockEntity()) {
			BlockEntity blockEntity = ((BlockEntityProvider)(state.getBlock())).createBlockEntity(BlockPos.fromLong(pos), state);
			if (blockEntity != null) this.queueBlockEntity(pos, blockEntity);
		}
		else {
			this.queuedBlockEntities.remove(pos);
		}
	}

	public void queueReplacement(BlockPos pos, BlockState from, BlockState to) {
		this.queueReplacement(pos.asLong(), from, to);
	}

	public void queueReplacement(long pos, BlockState from, BlockState to) {
		this.queueBlock(pos, to);
	}

	public void queueBlockEntity(long pos, BlockEntity blockEntity) {
		this.queuedBlockEntities.put(pos, blockEntity);
	}

	public void queueBlockEntity(BlockPos pos, BlockEntity blockEntity) {
		this.queueBlockEntity(pos.asLong(), blockEntity);
	}

	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return this.getBlockEntity(pos.asLong());
	}

	public @Nullable BlockEntity getBlockEntity(long pos) {
		return this.queuedBlockEntities.get(pos);
	}

	public void placeQueuedBlocks(WorldAccess world) {
		if (!this.queuedBlocks.isEmpty()) {
			BlockPos.Mutable pos = new BlockPos.Mutable();
			for (
				ObjectIterator<Long2ObjectMap.Entry<BlockState>> iterator = this.queuedBlocks.long2ObjectEntrySet().fastIterator();
				iterator.hasNext();
			) {
				Long2ObjectMap.Entry<BlockState> entry = iterator.next();
				world.setBlockState(pos.set(entry.getLongKey()), entry.getValue(), this.flags);
			}
			if (!this.queuedBlockEntities.isEmpty()) for (
				ObjectIterator<Long2ObjectMap.Entry<BlockEntity>> iterator = this.queuedBlockEntities.long2ObjectEntrySet().fastIterator();
				iterator.hasNext();
			) {
				Long2ObjectMap.Entry<BlockEntity> entry = iterator.next();
				BlockEntity worldBlockEntity = world.getBlockEntity(pos.set(entry.getLongKey()));
				if (worldBlockEntity != null) {
					BlockEntity queuedBlockEntity = entry.getValue();
					worldBlockEntity.readNbt(queuedBlockEntity.createNbt());
				}
			}
			/*
			if ((this.flags & Block.SKIP_LIGHTING_UPDATES) != 0 && world instanceof World) {
				LightingProvider lightManager = world.getLightingProvider();
				for (
					LongIterator iterator = this.queuedBlocks.keySet().iterator();
					iterator.hasNext();
				) {
					lightManager.checkBlock(pos.set(iterator.nextLong()));
				}
			}
			*/
		}
	}

	public int blockCount() {
		return this.queuedBlocks.size();
	}

	public BlockState getBlockStateOrNull(BlockPos pos) {
		return this.queuedBlocks.get(pos.asLong());
	}

	public BlockState getBlockState(BlockPos pos) {
		BlockState state = this.queuedBlocks.get(pos.asLong());
		return state != null ? state : BlockStates.AIR;
	}

	public BlockState getBlockState(BlockPos pos, BlockView fallback) {
		BlockState state = this.queuedBlocks.get(pos.asLong());
		return state != null ? state : fallback.getBlockState(pos);
	}

	@Override
	public String toString() {
		return (
			TypeFormatter.appendSimpleClassUnchecked(
				new StringBuilder(32),
				this.getClass()
			)
			.append(": { flags: ").append(this.flags)
			.append(", size: ").append(this.queuedBlocks.size())
			.append(" }")
			.toString()
		);
	}

	@TestOnly
	@SuppressWarnings("unused")
	public Object[] intellij_childrenArray() {
		return new Object[] {
			Map.entry("flags", this.flags),
			Map.entry("queuedBlocks", intellij_decodePositions(this.queuedBlocks)),
			Map.entry("queuedBlockEntities", intellij_decodePositions(this.queuedBlockEntities))
		};
	}

	public static <T> Object intellij_decodePositions(Long2ObjectMap<T> map) {
		if (map == null) return "null";
		int size = map.size();
		@SuppressWarnings("unchecked")
		QueuedObject<T>[] blocks = new QueuedObject[size];
		int index = 0;
		for (
			ObjectIterator<Long2ObjectMap.Entry<T>> iterator = (
				Long2ObjectMaps.fastIterator(map)
			);
			iterator.hasNext();
		) {
			Long2ObjectMap.Entry<T> entry = iterator.next();
			blocks[index++] = new QueuedObject<>(entry.getLongKey(), entry.getValue());
		}
		assert index == blocks.length;
		return blocks;
	}

	public static record QueuedObject<T>(int x, int y, int z, long packed, T object) {

		public QueuedObject(long packed, T state) {
			this(
				BlockPos.unpackLongX(packed),
				BlockPos.unpackLongY(packed),
				BlockPos.unpackLongZ(packed),
				packed,
				state
			);
		}

		public QueuedObject(int x, int y, int z, T state) {
			this(x, y, z, BlockPos.asLong(x, y, z), state);
		}
	}
}