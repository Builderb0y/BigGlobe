package builderb0y.bigglobe.versions;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

public class WorldVersions {

	#if MC_VERSION <= MC_1_19_2
		public static void scheduleBlockTick(WorldAccess world, BlockPos pos, Block block, int delay) {
			world.createAndScheduleBlockTick(pos, block, delay);
		}

		public static void scheduleFluidTick(WorldAccess world, BlockPos pos, Fluid fluid, int delay) {
			world.createAndScheduleFluidTick(pos, fluid, delay);
		}
	#else
		public static void scheduleBlockTick(WorldAccess world, BlockPos pos, Block block, int delay) {
			world.scheduleBlockTick(pos, block, delay);
		}

		public static void scheduleFluidTick(WorldAccess world, BlockPos pos, Fluid fluid, int delay) {
			world.scheduleFluidTick(pos, fluid, delay);
		}
	#endif
}