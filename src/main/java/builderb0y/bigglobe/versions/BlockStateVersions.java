package builderb0y.bigglobe.versions;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class BlockStateVersions {

	public static boolean isReplaceable(BlockState state) {
		return state.isReplaceable();
	}

	public static boolean canSpawnInside(BlockState state) {
		return state.getBlock().canMobSpawnInside(state);
	}

	public static boolean isOpaqueFullCube(BlockState state, BlockView world, BlockPos pos) {
		#if MC_VERSION >= MC_1_21_2
			return state.isOpaqueFullCube();
		#else
			return state.isOpaqueFullCube(world, pos);
		#endif
	}

	public static int getOpacity(BlockState state, BlockView world, BlockPos pos) {
		#if MC_VERSION >= MC_1_21_2
			return state.getOpacity();
		#else
			return state.getOpacity(world, pos);
		#endif
	}
}