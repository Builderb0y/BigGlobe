package builderb0y.bigglobe.versions;

import net.minecraft.block.BlockState;

import builderb0y.bigglobe.blocks.BigGlobeBlockTags;

public class MaterialVersions {

	public static boolean isReplaceable(BlockState state) {
		return state.isReplaceable();
	}

	public static boolean isReplaceableOrPlant(BlockState state) {
		return isReplaceable(state) || state.isIn(BigGlobeBlockTags.REPLACEABLE_PLANTS);
	}
}