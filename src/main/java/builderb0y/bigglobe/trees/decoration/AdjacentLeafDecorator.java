package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.LeafDecorator;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.util.Directions;

public class AdjacentLeafDecorator extends LeafDecorator {

	public final @Chance double chance;

	public AdjacentLeafDecorator(@VerifyNullable RandomList<BlockState> leaf_states, double chance) {
		super(leaf_states);
		this.chance = chance;
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos pos, BlockState state) {
		if (Permuter.nextChancedBoolean(generator.random, this.chance)) {
			this.placeAt(generator, pos.mutableCopy().move(Permuter.choose(generator.random, Directions.ALL)), 1);
		}
	}
}