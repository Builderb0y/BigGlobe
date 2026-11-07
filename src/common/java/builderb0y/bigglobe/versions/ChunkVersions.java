package builderb0y.bigglobe.versions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ChunkVersions {

	public static void setBlockState(ChunkAccess chunk, BlockPos pos, BlockState state, int flags) {
		chunk.setBlockState(pos, state, flags);
	}

	public static void postProcess(ChunkAccess chunk, BlockPos pos) {
		//single uppercase/lowercase character difference.
		#if MC_VERSION >= MC_26_2_0
			chunk.markPosForPostProcessing(pos);
		#else
			chunk.markPosForPostprocessing(pos);
		#endif
	}
}