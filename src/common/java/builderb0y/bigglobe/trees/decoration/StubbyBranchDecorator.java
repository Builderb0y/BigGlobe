package builderb0y.bigglobe.trees.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.LeafDecorator;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.util.Directions;

public class StubbyBranchDecorator extends LeafDecorator {

	public final @Chance double chance;

	public StubbyBranchDecorator(@VerifyNullable RandomList<BlockState> leaf_states, double chance) {
		super(leaf_states);
		this.chance = chance;
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos pos, BlockState state) {
		if (Permuter.nextChancedBoolean(generator.random, this.chance)) {
			Direction offsetDirection = Permuter.choose(generator.random, Directions.ALL);
			BlockPos offsetPos = pos.relative(offsetDirection);
			if (generator.canTrunkReplace(generator.worldQueue.getBlockState(offsetPos))) {
				generator.worldQueue.setBlockState(offsetPos, generator.palette.woodState(generator.random, offsetDirection.getAxis()));
				BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
				for (Direction direction : Directions.ALL) {
					this.placeAt(generator, mutablePos.set(offsetPos).move(direction), 1);
				}
			}
		}
	}
}