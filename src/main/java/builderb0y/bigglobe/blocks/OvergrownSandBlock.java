package builderb0y.bigglobe.blocks;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class OvergrownSandBlock extends FallingBlock implements Fertilizable {

	public OvergrownSandBlock(Settings settings) {
		super(settings);
	}

	@Override
	public int getColor(BlockState state, BlockView world, BlockPos pos) {
		return 0xDBD3A0;
	}

	@Override
	public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
		return true;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		((GrassBlock)(Blocks.GRASS_BLOCK)).grow(world, random, pos, state);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		BlockPos upPos = pos.up();
		if (world.getBlockState(upPos).isOpaqueFullCube(world, upPos)) {
			world.setBlockState(pos, BlockStates.SAND);
		}
	}
}