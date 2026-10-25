package builderb0y.bigglobe.util.coordinators;

import net.minecraft.core.BlockPos;

public abstract class ScratchPosCoordinator implements Coordinator {

	public final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
}