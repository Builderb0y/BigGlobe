package builderb0y.bigglobe.util.coordinators;

import net.minecraft.util.math.BlockPos;

public abstract class ScratchPosCoordinator implements Coordinator {

	public final BlockPos.Mutable scratchPos = new BlockPos.Mutable();
}