package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.itemdefs.BigGlobeItems;
import builderb0y.bigglobe.versions.ActionResultVersions;

import static builderb0y.bigglobe.blockdefs.BigGlobeBlocks.SPELUNKING_ROPE;

public class RopeAnchorBlock extends HorizontalDirectionalBlock {

	public static final BooleanProperty HAS_ROPE = BooleanProperty.create("has_rope");
	public static final VoxelShape EMPTY_SHAPE = Shapes.or(
		Shapes.create(0.375D, 0.0D, 0.375D, 0.625D, 0.375D, 0.625D),
		Shapes.create(0.25D, 0.375D, 0.25D, 0.75D, 0.5D, 0.75D)
	);

	public static final MapCodec<RopeAnchorBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(RopeAnchorBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public RopeAnchorBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(HAS_ROPE, Boolean.FALSE));
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void affectNeighborsAfterRemoval(
		BlockState state,
		ServerLevel world,
		BlockPos pos,

		boolean moved
	) {
		if (state.getValue(HAS_ROPE)) {
			Direction direction = state.getValue(FACING);
			world.scheduleTick(
				new BlockPos(
					pos.getX() + direction.getStepX(),
					pos.getY() - 1,
					pos.getZ() + direction.getStepZ()
				),
				SPELUNKING_ROPE,
				SPELUNKING_ROPE.getDelayAfterPlace()
			);
		}
		super.affectNeighborsAfterRemoval(state, world, pos, moved);
	}

	@Override

	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

		if (stack.isEmpty() && player.isShiftKeyDown()) {
			if (this.retractRopeAuto(world, pos, state, player)) {
				if (!world.isClientSide() && !player.isCreative()) {
					player.getInventory().add(new ItemStack(BigGlobeItems.SPELUNKING_ROPE));
				}
				return ActionResultVersions.ITEM_SUCCESS;
			}
			else {
				return ActionResultVersions.ITEM_FAIL;
			}
		}
		else if (stack.getItem() == BigGlobeItems.SPELUNKING_ROPE) {
			if (this.placeRopes(world, pos, state, player, stack)) {
				SPELUNKING_ROPE.playPlacementSound(player, world, pos);
				return ActionResultVersions.ITEM_SUCCESS;
			}
			else {
				return ActionResultVersions.ITEM_FAIL;
			}
		}
		return ActionResultVersions.ITEM_PASS;
	}

	public boolean placeRopes(Level world, BlockPos anchorPos, BlockState anchorState, Player player, ItemStack heldItem) {
		Direction direction = anchorState.getValue(FACING);
		BlockState toPlace = SPELUNKING_ROPE.defaultBlockState().setValue(FACING, direction.getOpposite());
		boolean placed = false;
		if (!anchorState.getValue(HAS_ROPE)) {
			if (world.isClientSide()) {
				return true;
			}
			else {
				world.setBlockAndUpdate(anchorPos, anchorState.setValue(HAS_ROPE, Boolean.TRUE));
				placed = true;
				if (!player.isCreative()) {
					heldItem.shrink(1);
					if (heldItem.isEmpty()) {
						return true;
					}
				}
			}
		}
		BlockPos.MutableBlockPos mutablePos = anchorPos.mutable().move(direction);
		if (SPELUNKING_ROPE.isExtrusionClear(world, mutablePos, direction.getOpposite())) {
			mutablePos.setY(mutablePos.getY() - 1);
			if (SPELUNKING_ROPE.placeRopesAuto(world, mutablePos, toPlace, player, heldItem)) {
				placed = true;
			}
		}
		return placed;
	}

	public boolean retractRopeAuto(Level world, BlockPos anchorPos, BlockState anchorState, Player player) {
		if (world.isClientSide()) {
			return this.retractRopeSimulate(anchorState);
		}
		else if (player.isCreative()) {
			return this.retractRopeCreative(world, anchorPos, anchorState, player);
		}
		else {
			return this.retractRopeSurvival(world, anchorPos, anchorState, player);
		}
	}

	public boolean retractRopeSurvival(Level world, BlockPos anchorPos, BlockState anchorState, Player player) {
		if (!anchorState.getValue(HAS_ROPE)) return false;
		Direction direction = anchorState.getValue(FACING);
		BlockState toRemove = SPELUNKING_ROPE.defaultBlockState().setValue(FACING, direction.getOpposite());
		BlockPos.MutableBlockPos mutablePos = anchorPos.mutable().move(direction.getStepX(), -1, direction.getStepZ());
		ChunkAccess chunk = world.getChunk(mutablePos);
		if (chunk.getBlockState(mutablePos) != toRemove) {
			world.setBlockAndUpdate(anchorPos, anchorState.setValue(HAS_ROPE, Boolean.FALSE));
			return true;
		}
		do mutablePos.setY(mutablePos.getY() - 1);
		while (chunk.getBlockState(mutablePos) == toRemove);
		mutablePos.setY(mutablePos.getY() + 1);
		world.setBlockAndUpdate(mutablePos, Blocks.AIR.defaultBlockState());
		return true;
	}

	public boolean retractRopeCreative(Level world, BlockPos anchorPos, BlockState anchorState, Player player) {
		if (!anchorState.getValue(HAS_ROPE)) return false;
		world.setBlockAndUpdate(anchorPos, anchorState.setValue(HAS_ROPE, Boolean.FALSE));
		Direction direction = anchorState.getValue(FACING);
		BlockState toRemove = SPELUNKING_ROPE.defaultBlockState().setValue(FACING, direction.getOpposite());
		BlockPos.MutableBlockPos mutablePos = anchorPos.mutable().move(direction.getStepX(), -1, direction.getStepZ());
		ChunkAccess chunk = world.getChunk(mutablePos);
		while (chunk.getBlockState(mutablePos) == toRemove) {
			world.setBlockAndUpdate(mutablePos, Blocks.AIR.defaultBlockState());
			mutablePos.setY(mutablePos.getY() - 1);
		}
		return true;
	}

	public boolean retractRopeSimulate(BlockState anchorState) {
		return anchorState.getValue(HAS_ROPE);
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
		return (
			direction == Direction.DOWN
			&& !this.canPlaceOn(world, neighborPos, neighborState)
				? Blocks.AIR.defaultBlockState()
				: state
		);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		BlockPos downPos = pos.below();
		return this.canPlaceOn(world, downPos, world.getBlockState(downPos));
	}

	public boolean canPlaceOn(LevelReader world, BlockPos downPos, BlockState downState) {
		return downState.isFaceSturdy(world, downPos, Direction.UP, SupportType.CENTER);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return EMPTY_SHAPE;
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HAS_ROPE);
	}
}