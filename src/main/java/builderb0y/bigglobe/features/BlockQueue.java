package builderb0y.bigglobe.features;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.AbstractObject2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.TestOnly;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.light.LightingProvider;

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
	public int flags;

	public BlockQueue(int flags) {
		this.flags = flags;
	}

	public BlockQueue(boolean causeBlockUpdates) {
		this.flags = (
			causeBlockUpdates
			? Block.NOTIFY_ALL       | Block.SKIP_LIGHTING_UPDATES
			: Block.NOTIFY_LISTENERS | Block.SKIP_LIGHTING_UPDATES
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
			if ((this.flags & Block.SKIP_LIGHTING_UPDATES) != 0 && world instanceof World) {
				LightingProvider lightManager = world.getLightingProvider();
				for (
					LongIterator iterator = this.queuedBlocks.keySet().iterator();
					iterator.hasNext();
				) {
					lightManager.checkBlock(pos.set(iterator.nextLong()));
				}
			}
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
	public Object[] intellij_childrenArray() {
		Object[] children = new Object[this.queuedBlocks.size() + 2];
		int index = 0;
		children[index++] = new AbstractObject2ObjectMap.BasicEntry<>("queuedBlocks", this.queuedBlocks);
		children[index++] = new AbstractObject2ObjectMap.BasicEntry<>("flags", this.flags);
		for (ObjectBidirectionalIterator<Long2ObjectMap.Entry<BlockState>> iterator = this.queuedBlocks.long2ObjectEntrySet().fastIterator(); iterator.hasNext();) {
			Long2ObjectMap.Entry<BlockState> entry = iterator.next();
			children[index++] = new AbstractObject2ObjectMap.BasicEntry<>(BlockPos.fromLong(entry.getLongKey()), entry.getValue());
		}
		assert index == children.length;
		return children;
	}
}