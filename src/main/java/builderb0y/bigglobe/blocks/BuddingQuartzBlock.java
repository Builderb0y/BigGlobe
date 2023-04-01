package builderb0y.bigglobe.blocks;

import net.minecraft.block.*;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import builderb0y.bigglobe.util.Directions;

public class BuddingQuartzBlock extends BuddingAmethystBlock {

	public BuddingQuartzBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (random.nextInt(GROW_CHANCE) != 0) {
			return;
		}
		Direction direction = Directions.ALL[random.nextInt(Directions.ALL.length)];
		BlockPos adjacentPos = pos.offset(direction);
		BlockState adjacentState = world.getBlockState(adjacentPos);
		Block blockToPlace;
		if (canGrowIn(adjacentState)) {
			blockToPlace = BigGlobeBlocks.SMALL_QUARTZ_BUD;
		}
		else if (adjacentState.isOf(BigGlobeBlocks.SMALL_QUARTZ_BUD) && adjacentState.get(AmethystClusterBlock.FACING) == direction) {
			blockToPlace = BigGlobeBlocks.MEDIUM_QUARTZ_BUD;
		}
		else if (adjacentState.isOf(BigGlobeBlocks.MEDIUM_QUARTZ_BUD) && adjacentState.get(AmethystClusterBlock.FACING) == direction) {
			blockToPlace = BigGlobeBlocks.LARGE_QUARTZ_BUD;
		}
		else if (adjacentState.isOf(BigGlobeBlocks.LARGE_QUARTZ_BUD) && adjacentState.get(AmethystClusterBlock.FACING) == direction) {
			blockToPlace = BigGlobeBlocks.QUARTZ_CLUSTER;
		}
		else {
			blockToPlace = null;
		}
		if (blockToPlace != null) {
			BlockState stateToPlace = (
				blockToPlace
				.getDefaultState()
				.with(AmethystClusterBlock.FACING, direction)
				.with(AmethystClusterBlock.WATERLOGGED, adjacentState.getFluidState().getFluid() == Fluids.WATER)
			);
			world.setBlockState(adjacentPos, stateToPlace);
		}
	}
}