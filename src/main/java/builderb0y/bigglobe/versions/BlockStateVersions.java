package builderb0y.bigglobe.versions;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

public class BlockStateVersions {

	public static boolean isReplaceable(BlockState state) {
		#if MC_VERSION < MC_1_20_0
			return state.getMaterial().isReplaceable();
		#else
			return state.isReplaceable();
		#endif
	}

	public static boolean canSpawnInside(BlockState state) {
		#if MC_VERSION < MC_1_20_0
			return state.getBlock().canMobSpawnInside();
		#else
			return state.getBlock().canMobSpawnInside(state);
		#endif
	}

	public static <C extends Comparable<C>> BlockState withIfExists(BlockState state, Property<C> property, C value) {
		#if MC_VERSION <= MC_1_19_2
			return state.contains(property) ? state.with(property, value) : state;
		#else
			return state.withIfExists(property, value);
		#endif
	}
}