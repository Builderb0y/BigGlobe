package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.blockdefs.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class OvergrownSandBlock extends FallingBlock implements BonemealableBlock {

	#if MC_VERSION < MC_26_3_0

		public static final MapCodec<OvergrownSandBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(OvergrownSandBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec codec() {
			return CODEC;
		}

	#endif

	public OvergrownSandBlock(Properties settings) {
		super(settings);
	}

	@Override
	public int getDustColor(BlockState state, BlockGetter world, BlockPos pos) {
		return 0xDBD3A0;
	}

	@Override
	public boolean isValidBonemealTarget(
		LevelReader world,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(
		Level world,
		RandomSource random,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		return true;
	}

	@Override
	public void performBonemeal(
		ServerLevel world,
		RandomSource random,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		((GrassBlock)(Blocks.GRASS_BLOCK)).performBonemeal(world, random, pos, state #if MC_VERSION >= MC_26_3_0 , source #endif);
	}

	@Override
	@Deprecated
	public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		BlockPos upPos = pos.above();
		if (BlockStateVersions.isOpaqueFullCube(world.getBlockState(upPos), world, upPos)) {
			world.setBlockAndUpdate(pos, BlockStates.SAND);
		}
	}
}