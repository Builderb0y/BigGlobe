package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.util.Directions;

public class RandomWalkLeafDecorator extends ConfiguredLeafDecorator {

	public RandomWalkLeafDecorator(boolean is_trunk, @VerifyNullable RandomList<BlockState> leaf_states) {
		super(is_trunk, leaf_states);
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos branchPos, BlockState branchState) {
		if (this.is_trunk && generator.trunk.currentFracY < generator.branches.startFracY) return;
		double radius = generator.trunk.currentRadius * 0.5D + 2.0D;
		int walks = Permuter.roundRandomlyI(generator.random, radius * radius * radius);
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		for (int walk = 1; walk <= walks; walk++) {
			mutablePos.set(branchPos);
			int steps = Permuter.roundRandomlyI(generator.random, radius);
			for (int step = 1; step <= steps; step++) {
				this.placeAt(generator, mutablePos.move(Permuter.choose(generator.random, Directions.ALL)), step);
			}
		}
	}
}