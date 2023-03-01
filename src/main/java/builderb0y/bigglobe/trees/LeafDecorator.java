package builderb0y.bigglobe.trees;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.decoration.BlockDecorator;

public abstract class LeafDecorator implements BlockDecorator {

	public final @VerifyNullable RandomList<@UseName("state") BlockState> leaf_states;

	public LeafDecorator(@VerifyNullable RandomList<BlockState> leaf_states) {
		this.leaf_states = leaf_states;
	}

	public boolean placeAt(TreeGenerator generator, BlockPos.Mutable pos, int distance) {
		BlockState existingState = generator.worldQueue.getBlockState(pos);
		if (generator.canLeavesReplace(existingState)) {
			distance = Math.min(distance, 7);
			BlockState toPlace;
			if (this.leaf_states != null) {
				toPlace = this.leaf_states.getRandomElement(generator.random);
				if (toPlace.contains(LeavesBlock.DISTANCE)) {
					toPlace = toPlace.with(LeavesBlock.DISTANCE, distance);
				}
				if (toPlace.contains(LeavesBlock.PERSISTENT)) {
					toPlace = toPlace.with(LeavesBlock.PERSISTENT, Boolean.FALSE);
				}
			}
			else {
				toPlace = generator.palette.getLeaves(distance, false);
			}
			generator.queueAndDecorateLeaf(pos, toPlace);
			return true;
		}
		else {
			return false;
		}
	}
}