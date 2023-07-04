package builderb0y.bigglobe.versions;

import net.minecraft.util.math.BlockPos;

public class BlockPosVersions {

	public static BlockPos floor(double x, double y, double z) {
		return BlockPos.ofFloored(x, y, z);
	}
}