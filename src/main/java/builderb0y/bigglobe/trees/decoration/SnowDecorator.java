package builderb0y.bigglobe.trees.decoration;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.util.math.Direction;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToFloatScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.trees.TreeGenerator;

@RecordLike("chance")
public class SnowDecorator implements BlockDecorator {

	public final ColumnToFloatScript.Holder chance;
	public transient Long2FloatMap cache;

	public SnowDecorator(ColumnToFloatScript.Holder chance, int initialCapacity) {
		this.chance = chance;
		this.cache = new Long2FloatOpenHashMap(initialCapacity);
	}

	public SnowDecorator(ColumnToFloatScript.Holder chance) {
		this(chance, 0);
	}

	public float getSnowChance(TreeGenerator generator, int x, int z) {
		ScriptedColumn column = generator.column;
		ColumnToFloatScript.Holder chance = this.chance;
		return this.cache.computeIfAbsent(ColumnPos.pack(x, z), (long key) -> {
			column.setPos((int)(key), (int)(key >>> 32));
			return chance.get(column);
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
		return new SnowDecorator(this.chance, 48);
	}
}