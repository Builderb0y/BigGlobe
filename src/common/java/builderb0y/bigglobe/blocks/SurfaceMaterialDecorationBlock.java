package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import builderb0y.bigglobe.blockdefs.BlockStates;

public abstract class SurfaceMaterialDecorationBlock extends Block implements SimpleWaterloggedBlock {

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public abstract MapCodec codec();

	public SurfaceMaterialDecorationBlock(BlockBehaviour.Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE));
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public abstract VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context);

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		BlockPos downPos = pos.below();
		return world.getBlockState(downPos).isFaceSturdy(world, downPos, Direction.UP);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).isSourceOfType(Fluids.WATER));
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
		if (state.getValue(BlockStateProperties.WATERLOGGED)) {
			tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		}
		if (direction == Direction.DOWN && !this.canSurvive(state, world, pos)) {
			return BlockStates.AIR;
		}
		return state;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public FluidState getFluidState(BlockState state) {
		return (
			state.getValue(BlockStateProperties.WATERLOGGED)
				? Fluids.WATER.getSource(false)
				: Fluids.EMPTY.defaultFluidState()
		);
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BlockStateProperties.WATERLOGGED);
	}
}