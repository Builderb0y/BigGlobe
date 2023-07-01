package builderb0y.bigglobe.blocks;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import builderb0y.bigglobe.features.SingleBlockFeature;

public class ChorusSporeBlock extends PlantBlock implements Fertilizable {

	public final Block growInto;
	public final VoxelShape shape;

	public ChorusSporeBlock(Settings settings, Block into, VoxelShape shape) {
		super(settings);
		this.growInto = into;
		this.shape = shape;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.shape;
	}

	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
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
		SingleBlockFeature.place(world, pos, this.growInto.getDefaultState(), SingleBlockFeature.IS_REPLACEABLE);
	}
}