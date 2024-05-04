package builderb0y.bigglobe.versions;

import net.minecraft.block.BlockState;

public class BlockStateVersions {

	public static boolean isReplaceable(BlockState state) {
		return state.isReplaceable();
	}

	public static boolean canSpawnInside(BlockState state) {
		return state.getBlock().canMobSpawnInside(state);
	}
}