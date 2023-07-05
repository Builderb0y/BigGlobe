package builderb0y.bigglobe.versions;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

import builderb0y.bigglobe.blocks.BigGlobeBlockTags;

public class BlockStateVersions {

	public static boolean isReplaceable(BlockState state) {
		return state.isReplaceable();
	}

	public static boolean isReplaceableOrPlant(BlockState state) {
		return isReplaceable(state) || state.isIn(BigGlobeBlockTags.REPLACEABLE_PLANTS);
	}

	public static boolean canSpawnInside(BlockState state) {
		return state.getBlock().canMobSpawnInside(state);
	}

	public static <C extends Comparable<C>> BlockState withIfExists(BlockState state, Property<C> property, C value) {
		return state.withIfExists(property, value);
	}
}