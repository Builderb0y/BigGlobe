package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class OvergrownSandBlock extends FallingBlock implements BonemealableBlock {

	public static final MapCodec<OvergrownSandBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(OvergrownSandBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

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

	) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
		((GrassBlock)(Blocks.GRASS_BLOCK)).performBonemeal(world, random, pos, state);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		BlockPos upPos = pos.above();
		if (BlockStateVersions.isOpaqueFullCube(world.getBlockState(upPos), world, upPos)) {
			world.setBlockAndUpdate(pos, BlockStates.SAND);
		}
	}
}