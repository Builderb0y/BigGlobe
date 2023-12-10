package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class ShortGrassBlock extends PlantBlock implements Fertilizable {

	public static final VoxelShape SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D);

	public ShortGrassBlock(Settings settings) {
		super(settings);
	}

	#if MC_VERSION >= MC_1_20_3
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			throw new NotImplementedException();
		}
	#endif

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public boolean isFertilizable(
		#if (MC_VERSION <= MC_1_19_2) BlockView #else WorldView #endif world,
		BlockPos pos,
		BlockState state
		#if MC_VERSION < MC_1_20_2 , boolean isClient #endif
	) {
		return true;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		BlockState replacement = Blocks.#if MC_VERSION >= MC_1_20_3 SHORT_GRASS #else GRASS #endif.getDefaultState();
		if (replacement.canPlaceAt(world, pos)) {
			world.setBlockState(pos, replacement, Block.NOTIFY_ALL);
		}
	}
}