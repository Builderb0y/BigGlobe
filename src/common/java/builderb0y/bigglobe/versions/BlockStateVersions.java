package builderb0y.bigglobe.versions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockStateVersions {

	public static boolean isReplaceable(BlockState state) {
		return state.canBeReplaced();
	}

	public static boolean canSpawnInside(BlockState state) {
		return state.getBlock().isPossibleToRespawnInThis(state);
	}

	public static boolean isOpaqueFullCube(BlockState state, BlockGetter world, BlockPos pos) {
		return state.isSolidRender();
	}

	public static int getOpacity(BlockState state, BlockGetter world, BlockPos pos) {
		return state.getLightDampening();
	}

	public static VoxelShape getCullingShape(BlockState state, BlockGetter world, BlockPos pos) {
		return state.getOcclusionShape();
	}
}