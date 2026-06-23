package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.blockdefs.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

/**
this block exists to be generated in the nether, surrounded by magma blocks.
as soon as one of those magma blocks is removed, this block turns into actual lava.
the reason why we don't generate actual lava initially is because lava doesn't cull adjacent faces,
which results in a lot of unnecessary geometry being rendered.
*/
public class HiddenLavaBlock extends Block {

	public static final MapCodec<HiddenLavaBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(HiddenLavaBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public HiddenLavaBlock(Properties settings) {
		super(settings);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState updateShape(

		BlockState state,
		LevelReader world,
		ScheduledTickAccess tickView,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random

	) {
		return Block.isShapeFullBlock(neighborState.getOcclusionShape()) ? state : BlockStates.LAVA;
	}
}