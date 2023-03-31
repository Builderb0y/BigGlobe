package builderb0y.bigglobe.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

/**
this block exists to be generated in the nether, surrounded by magma blocks.
as soon as one of those magma blocks is removed, this block turns into actual lava.
the reason why we don't generate actual lava initially is because lava doesn't cull adjacent faces,
which results in a lot of unnecessary geometry being rendered.
*/
public class HiddenLavaBlock extends Block {

	public HiddenLavaBlock(Settings settings) {
		super(settings);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		return Block.isShapeFullCube(neighborState.getCullingShape(world, pos)) ? state : BlockStates.LAVA;
	}
}