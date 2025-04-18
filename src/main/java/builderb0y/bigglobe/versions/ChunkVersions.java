package builderb0y.bigglobe.versions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

public class ChunkVersions {

	public static void setBlockState(Chunk chunk, BlockPos pos, BlockState state, int flags) {
		#if MC_VERSION >= MC_1_21_5
			chunk.setBlockState(pos, state, flags);
		#else
			chunk.setBlockState(pos, state, (flags & Block.MOVED) != 0);
		#endif
	}
}