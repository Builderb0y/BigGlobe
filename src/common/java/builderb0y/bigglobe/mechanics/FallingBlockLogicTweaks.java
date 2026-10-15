package builderb0y.bigglobe.mechanics;

import java.util.*;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.FallingBlock_Access;
import builderb0y.bigglobe.util.Directions;

public class FallingBlockLogicTweaks {

	public static final WeakHashMap<ServerLevel, Object2LongMap<BlockPos>> RECENT_FALLS = new WeakHashMap<>();

	public static boolean canFall(ServerLevel world, BlockPos pos) {
		Object2LongMap<BlockPos> recentPositions;
		synchronized (RECENT_FALLS) {
			recentPositions = RECENT_FALLS.get(world);
		}
		if (recentPositions != null && !recentPositions.isEmpty()) {
			for (ObjectIterator<Object2LongMap.Entry<BlockPos>> iterator = recentPositions.object2LongEntrySet().iterator(); iterator.hasNext(); ) {
				Object2LongMap.Entry<BlockPos> entry = iterator.next();
				if (world.getGameTime() >= entry.getLongValue()) {
					iterator.remove();
					continue;
				}
				if (BigGlobeMath.squareL(entry.getKey().getX() - pos.getX(), entry.getKey().getY() - pos.getY(), entry.getKey().getZ() - pos.getZ()) < 16L * 16L) {
					return false;
				}
			}
		}
		return true;
	}

	public static void fall(ServerLevel world, BlockPos pos) {
		pos = pos.immutable();
		synchronized (RECENT_FALLS) {
			RECENT_FALLS.computeIfAbsent(world, (ServerLevel _) -> new Object2LongOpenHashMap<>()).put(pos, world.getGameTime() + 20L);
		}
		int fallCount = world.getRandom().nextInt(16, 256);
		Set<BlockPos> toFall = new HashSet<>(fallCount << 1);
		List<BlockPos> queue = new ArrayList<>(fallCount);
		toFall.add(pos);
		queue.add(pos);
		for (int spread = 0; spread < fallCount && !queue.isEmpty(); spread++) {
			int index = world.getRandom().nextInt(queue.size());
			BlockPos center = index == queue.size() - 1 ? queue.remove(index) : queue.set(index, queue.removeLast());
			for (Direction direction : Directions.ALL) {
				BlockPos adjacent = center.relative(direction);
				if (!toFall.contains(adjacent) && world.getBlockState(adjacent).getBlock() instanceof FallingBlock) {
					toFall.add(adjacent);
					queue.add(adjacent);
				}
			}
		}
		queue.clear();
		queue.addAll(toFall);
		queue.sort(Comparator.comparingInt(BlockPos::getY));
		for (BlockPos blockPos : queue) {
			BlockState state = world.getBlockState(blockPos);
			if (state.getBlock() instanceof FallingBlock_Access access && FallingBlock.isFree(world.getBlockState(blockPos.below()))) {
				access.bigglobe_falling(FallingBlockEntity.fall(world, blockPos, state));
			}
		}
	}
}