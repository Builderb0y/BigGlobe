package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.LeafDecorator;
import builderb0y.bigglobe.trees.branches.BranchesConfig;

public abstract class ConfiguredLeafDecorator extends LeafDecorator {

	/**
	if this is set to true and we are currently decorating the trunk,
	then we will not place blocks below {@link BranchesConfig#startFracY}.
	this is mostly a workaround for leaves, which sometimes
	do not have enough branches to look good on small trees.
	so, we place leaves around the trunk too, but only above startFracY.
	*/
	public final boolean is_trunk;

	public ConfiguredLeafDecorator(boolean is_trunk, @VerifyNullable RandomList<BlockState> leaf_states) {
		super(leaf_states);
		this.is_trunk = is_trunk;
	}
}