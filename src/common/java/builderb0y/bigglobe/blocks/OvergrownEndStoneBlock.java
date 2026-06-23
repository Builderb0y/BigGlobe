package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.blockdefs.EndBlocks;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class OvergrownEndStoneBlock extends Block implements BonemealableBlock {

	public static final MapCodec<OvergrownEndStoneBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(OvergrownEndStoneBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public OvergrownEndStoneBlock(Properties settings) {
		super(settings);
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
		world.setBlockAndUpdate(pos, EndBlocks.CHORUS_NYLIUM.defaultBlockState());
	}
}