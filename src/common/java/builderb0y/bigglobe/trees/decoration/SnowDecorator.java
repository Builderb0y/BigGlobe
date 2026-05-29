package builderb0y.bigglobe.trees.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToFloatScript;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.trees.TreeGenerator;

public class SnowDecorator implements BlockDecorator {

	public final ColumnYToFloatScript.Catcher chance;

	public SnowDecorator(ColumnYToFloatScript.Catcher chance) {
		this.chance = chance;
	}

	public float getSnowChance(TreeGenerator generator, int x, int y, int z) {
		return this.chance.get(generator.columns.lookupColumn(x, z), y);
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos pos, BlockState state) {
		if (Block.isFaceFull(state.getCollisionShape(generator.worldQueue, pos), Direction.UP)) {
			if (Permuter.nextChancedBoolean(generator.random, this.getSnowChance(generator, pos.getX(), pos.getY() + 1, pos.getZ()))) {
				BlockPos up = pos.above();
				if (generator.worldQueue.getBlockState(up).isAir()) {
					generator.worldQueue.setBlockState(up, BlockStates.SNOW);
				}
			}
		}
	}
}