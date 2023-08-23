package builderb0y.bigglobe.blocks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.versions.WorldVersions;

import static builderb0y.bigglobe.blocks.BigGlobeBlocks.SPELUNKING_ROPE;

public class RopeAnchorBlock extends HorizontalFacingBlock {

	public static final BooleanProperty HAS_ROPE = BooleanProperty.of("has_rope");
	public static final VoxelShape EMPTY_SHAPE = VoxelShapes.union(
		VoxelShapes.cuboidUnchecked(0.375D, 0.0D, 0.375D, 0.625D, 0.375D, 0.625D),
		VoxelShapes.cuboidUnchecked(0.25D, 0.375D, 0.25D, 0.75D, 0.5D, 0.75D)
	);

	public RopeAnchorBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(HAS_ROPE, Boolean.FALSE));
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (state.get(HAS_ROPE) && newState != state) {
			Direction direction = state.get(FACING);
			WorldVersions.scheduleBlockTick(
				world,
				new BlockPos(
					pos.getX() + direction.getOffsetX(),
					pos.getY() - 1,
					pos.getZ() + direction.getOffsetZ()
				),
				SPELUNKING_ROPE,
				SPELUNKING_ROPE.getFallDelay()
			);
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		ItemStack stack = player.getStackInHand(hand);
		if (stack.isEmpty() && player.isSneaking()) {
			if (this.retractRopeAuto(world, pos, state, player)) {
				if (!world.isClient && !player.isCreative()) {
					player.getInventory().insertStack(new ItemStack(BigGlobeItems.SPELUNKING_ROPE));
				}
				return ActionResult.SUCCESS;
			}
			else {
				return ActionResult.FAIL;
			}
		}
		else if (stack.getItem() == BigGlobeItems.SPELUNKING_ROPE) {
			if (this.placeRopes(world, pos, state, player, stack)) {
				SPELUNKING_ROPE.playPlacementSound(player, world, pos);
				return ActionResult.SUCCESS;
			}
			else {
				return ActionResult.FAIL;
			}
		}
		return ActionResult.PASS;
	}

	public boolean placeRopes(World world, BlockPos anchorPos, BlockState anchorState, PlayerEntity player, ItemStack heldItem) {
		Direction direction = anchorState.get(FACING);
		BlockState toPlace = SPELUNKING_ROPE.getDefaultState().with(FACING, direction.getOpposite());
		boolean placed = false;
		if (!anchorState.get(HAS_ROPE)) {
			if (world.isClient) {
				return true;
			}
			else {
				world.setBlockState(anchorPos, anchorState.with(HAS_ROPE, Boolean.TRUE));
				placed = true;
				if (!player.isCreative()) {
					heldItem.decrement(1);
					if (heldItem.isEmpty()) {
						return true;
					}
				}
			}
		}
		BlockPos.Mutable mutablePos = anchorPos.mutableCopy().move(direction);
		if (SPELUNKING_ROPE.isExtrusionClear(world, mutablePos, direction.getOpposite())) {
			mutablePos.setY(mutablePos.getY() - 1);
			if (SPELUNKING_ROPE.placeRopesAuto(world, mutablePos, toPlace, player, heldItem)) {
				placed = true;
			}
		}
		return placed;
	}

	public boolean retractRopeAuto(World world, BlockPos anchorPos, BlockState anchorState, PlayerEntity player) {
		if (world.isClient) {
			return this.retractRopeSimulate(anchorState);
		}
		else if (player.isCreative()) {
			return this.retractRopeCreative(world, anchorPos, anchorState, player);
		}
		else {
			return this.retractRopeSurvival(world, anchorPos, anchorState, player);
		}
	}

	public boolean retractRopeSurvival(World world, BlockPos anchorPos, BlockState anchorState, PlayerEntity player) {
		if (!anchorState.get(HAS_ROPE)) return false;
		Direction direction = anchorState.get(FACING);
		BlockState toRemove = SPELUNKING_ROPE.getDefaultState().with(FACING, direction.getOpposite());
		BlockPos.Mutable mutablePos = anchorPos.mutableCopy().move(direction.getOffsetX(), -1, direction.getOffsetZ());
		Chunk chunk = world.getChunk(mutablePos);
		if (chunk.getBlockState(mutablePos) != toRemove) {
			world.setBlockState(anchorPos, anchorState.with(HAS_ROPE, Boolean.FALSE));
			return true;
		}
		do mutablePos.setY(mutablePos.getY() - 1);
		while (chunk.getBlockState(mutablePos) == toRemove);
		mutablePos.setY(mutablePos.getY() + 1);
		world.setBlockState(mutablePos, Blocks.AIR.getDefaultState());
		return true;
	}

	public boolean retractRopeCreative(World world, BlockPos anchorPos, BlockState anchorState, PlayerEntity player) {
		if (!anchorState.get(HAS_ROPE)) return false;
		world.setBlockState(anchorPos, anchorState.with(HAS_ROPE, Boolean.FALSE));
		Direction direction = anchorState.get(FACING);
		BlockState toRemove = SPELUNKING_ROPE.getDefaultState().with(FACING, direction.getOpposite());
		BlockPos.Mutable mutablePos = anchorPos.mutableCopy().move(direction.getOffsetX(), -1, direction.getOffsetZ());
		Chunk chunk = world.getChunk(mutablePos);
		while (chunk.getBlockState(mutablePos) == toRemove) {
			world.setBlockState(mutablePos, Blocks.AIR.getDefaultState());
			mutablePos.setY(mutablePos.getY() - 1);
		}
		return true;
	}

	public boolean retractRopeSimulate(BlockState anchorState) {
		return anchorState.get(HAS_ROPE);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		return (
			direction == Direction.DOWN
			&& !this.canPlaceOn(world, neighborPos, neighborState)
			? Blocks.AIR.getDefaultState()
			: state
		);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos downPos = pos.down();
		return this.canPlaceOn(world, downPos, world.getBlockState(downPos));
	}

	public boolean canPlaceOn(WorldView world, BlockPos downPos, BlockState downState) {
		return downState.isSideSolid(world, downPos, Direction.UP, SideShapeType.CENTER);
	}

	@Override
	@Nullable
	public BlockState getPlacementState(ItemPlacementContext context) {
		return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing());
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return EMPTY_SHAPE;
	}

	@Override
	public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HAS_ROPE);
	}

	#if MC_VERSION < MC_1_20_0
		@Override
		@Deprecated
		@SuppressWarnings("deprecation")
		public PistonBehavior getPistonBehavior(BlockState state) {
			return PistonBehavior.DESTROY;
		}
	#endif
}