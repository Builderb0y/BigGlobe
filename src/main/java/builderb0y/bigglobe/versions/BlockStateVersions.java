package builderb0y.bigglobe.versions;

import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.state.property.Property;

public class BlockStateVersions {

	public static boolean isReplaceable(BlockState state) {
		return state.getMaterial().isReplaceable();
	}

	public static boolean isReplaceableOrPlant(BlockState state) {
		return isReplaceable(state) || state.getMaterial() == Material.PLANT;
	}

	public static boolean canSpawnInside(BlockState state) {
		return state.getBlock().canMobSpawnInside();
	}

	public static <C extends Comparable<C>> BlockState withIfExists(BlockState state, Property<C> property, C value) {
		return state.contains(property) ? state.with(property, value) : state;
	}
}