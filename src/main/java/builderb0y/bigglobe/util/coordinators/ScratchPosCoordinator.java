package builderb0y.bigglobe.util.coordinators;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateSupplier;

public abstract class ScratchPosCoordinator implements Coordinator {

	public final BlockPos.Mutable scratchPos = new BlockPos.Mutable();

	@Override
	public void setBlockStateRelative(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		this.setBlockState(x, y, z, supplier.get(this.scratchPos.set(x, y, z)));
	}
}