package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToFloatScript;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.trees.TreeGenerator;

public class SnowDecorator implements BlockDecorator {

	public final ColumnYToFloatScript.Holder chance;

	public SnowDecorator(ColumnYToFloatScript.Holder chance) {
		this.chance = chance;
	}

	public float getSnowChance(TreeGenerator generator, int x, int y, int z) {
		return this.chance.get(generator.columns.lookupColumn(x, z), y);
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos pos, BlockState state) {
		if (Block.isFaceFullSquare(state.getCollisionShape(generator.worldQueue, pos), Direction.UP)) {
			if (Permuter.nextChancedBoolean(generator.random, this.getSnowChance(generator, pos.getX(), pos.getY() + 1, pos.getZ()))) {
				BlockPos up = pos.up();
				if (generator.worldQueue.getBlockState(up).isAir()) {
					generator.worldQueue.setBlockState(up, BlockStates.SNOW);
				}
			}
		}
	}
}