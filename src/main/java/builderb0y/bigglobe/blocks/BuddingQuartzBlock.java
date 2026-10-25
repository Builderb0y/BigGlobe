package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.util.Directions;

public class BuddingQuartzBlock extends BuddingAmethystBlock {

	public static final MapCodec<BuddingQuartzBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(BuddingQuartzBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public BuddingQuartzBlock(Properties settings) {
		super(settings);
	}

	@Override
	public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (random.nextInt(GROWTH_CHANCE) != 0) {
			return;
		}
		Direction direction = Directions.ALL[random.nextInt(Directions.ALL.length)];
		BlockPos adjacentPos = pos.relative(direction);
		BlockState adjacentState = world.getBlockState(adjacentPos);
		Block blockToPlace;
		if (canClusterGrowAtState(adjacentState)) {
			blockToPlace = BigGlobeBlocks.SMALL_QUARTZ_BUD;
		}
		else if (adjacentState.is(BigGlobeBlocks.SMALL_QUARTZ_BUD) && adjacentState.getValue(AmethystClusterBlock.FACING) == direction) {
			blockToPlace = BigGlobeBlocks.MEDIUM_QUARTZ_BUD;
		}
		else if (adjacentState.is(BigGlobeBlocks.MEDIUM_QUARTZ_BUD) && adjacentState.getValue(AmethystClusterBlock.FACING) == direction) {
			blockToPlace = BigGlobeBlocks.LARGE_QUARTZ_BUD;
		}
		else if (adjacentState.is(BigGlobeBlocks.LARGE_QUARTZ_BUD) && adjacentState.getValue(AmethystClusterBlock.FACING) == direction) {
			blockToPlace = BigGlobeBlocks.QUARTZ_CLUSTER;
		}
		else {
			blockToPlace = null;
		}
		if (blockToPlace != null) {
			BlockState stateToPlace = (
				blockToPlace
					.defaultBlockState()
					.setValue(AmethystClusterBlock.FACING, direction)
					.setValue(AmethystClusterBlock.WATERLOGGED, adjacentState.getFluidState().getType() == Fluids.WATER)
			);
			world.setBlockAndUpdate(adjacentPos, stateToPlace);
		}
	}
}