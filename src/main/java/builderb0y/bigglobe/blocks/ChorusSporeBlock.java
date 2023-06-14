package builderb0y.bigglobe.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import builderb0y.bigglobe.features.SingleBlockFeature;

public class ChorusSporeBlock extends PlantBlock implements Fertilizable {

	public final Block growInto;

	public ChorusSporeBlock(Settings settings, Block into) {
		super(settings);
		this.growInto = into;
	}

	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
		return true;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		SingleBlockFeature.place(world, pos, this.growInto.getDefaultState(), SingleBlockFeature.IS_REPLACEABLE);
	}
}