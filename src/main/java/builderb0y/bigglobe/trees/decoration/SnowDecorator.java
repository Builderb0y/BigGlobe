package builderb0y.bigglobe.trees.decoration;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.trees.TreeGenerator;

@RecordLike({}) //RecordDecoder.Factory requires at least 1 field by default, but we have none.
public class SnowDecorator implements BlockDecorator {

	public transient Long2FloatMap cache;

	public SnowDecorator(int initialCapacity) {
		this.cache = new Long2FloatOpenHashMap(initialCapacity);
	}

	public SnowDecorator() {
		this(0);
	}

	public float getSnowChance(TreeGenerator generator, int x, int z) {
		if (!(generator.anywhereColumn instanceof OverworldColumn column)) return 0.0F;
		return this.cache.computeIfAbsent((((long)(z)) << 32) | (((long)(x)) & 0xFFFF_FFFFL), (long key) -> {
			column.setPosUnchecked((int)(key), (int)(key >>> 32));
			return (float)(column.getSnowChance());
		});
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos pos, BlockState state) {
		if (Block.isFaceFullSquare(state.getCollisionShape(generator.worldQueue, pos), Direction.UP)) {
			if (Permuter.nextChancedBoolean(generator.random, this.getSnowChance(generator, pos.getX(), pos.getZ()))) {
				BlockPos up = pos.up();
				if (generator.worldQueue.getBlockState(up).isAir()) {
					generator.worldQueue.setBlockState(up, BlockStates.SNOW);
				}
			}
		}
	}

	@Override
	public BlockDecorator copyIfMutable() {
		return new SnowDecorator(48);
	}
}