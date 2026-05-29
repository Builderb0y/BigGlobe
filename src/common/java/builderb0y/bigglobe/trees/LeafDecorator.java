package builderb0y.bigglobe.trees;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.decoration.BlockDecorator;

public abstract class LeafDecorator implements BlockDecorator {

	public final @VerifyNullable RandomList<@UseName("state") BlockState> leaf_states;

	public LeafDecorator(@VerifyNullable RandomList<BlockState> leaf_states) {
		this.leaf_states = leaf_states;
	}

	public boolean placeAt(TreeGenerator generator, BlockPos.MutableBlockPos pos, int distance) {
		BlockState existingState = generator.worldQueue.getBlockState(pos);
		if (generator.canLeavesReplace(existingState)) {
			distance = Interpolator.clamp(1, 7, distance);
			BlockState toPlace;
			if (this.leaf_states != null) {
				toPlace = this.leaf_states.getRandomElement(generator.random);
				if (toPlace.hasProperty(LeavesBlock.DISTANCE)) {
					toPlace = toPlace.setValue(LeavesBlock.DISTANCE, distance);
				}
				if (toPlace.hasProperty(LeavesBlock.PERSISTENT)) {
					toPlace = toPlace.setValue(LeavesBlock.PERSISTENT, Boolean.FALSE);
				}
			}
			else {
				toPlace = generator.palette.leavesState(generator.random, distance, false, false);
			}
			generator.queueAndDecorateLeaf(pos, toPlace);
			return true;
		}
		else {
			return false;
		}
	}
}