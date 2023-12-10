package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class OvergrownEndStoneBlock extends Block implements Fertilizable {

	public OvergrownEndStoneBlock(Settings settings) {
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
		world.setBlockState(pos, BigGlobeBlocks.CHORUS_NYLIUM.getDefaultState());
	}
}