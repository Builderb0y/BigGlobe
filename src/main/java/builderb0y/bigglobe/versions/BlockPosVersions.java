package builderb0y.bigglobe.versions;

import net.minecraft.util.math.BlockPos;

public class BlockPosVersions {

	public static BlockPos floor(double x, double y, double z) {
		#if MC_VERSION <= MC_1_19_2
			return new BlockPos(x, y, z);
		#else
			return BlockPos.ofFloored(x, y, z);
		#endif
	}
}