package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class OvergrownEndStoneBlock extends Block implements Fertilizable {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<OvergrownEndStoneBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(OvergrownEndStoneBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public OvergrownEndStoneBlock(Settings settings) {
		super(settings);
	}

	@Override
	public boolean isFertilizable(
		WorldView world,
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