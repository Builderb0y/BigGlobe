package builderb0y.bigglobe.blocks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.mixins.FallingBlockEntity_DestroyOnLandingAccess;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.MaterialVersions;

public class SpelunkingRopeBlock extends FallingBlock {

	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final VoxelShape
		NORTH_SHAPE     = VoxelShapes.cuboidUnchecked(0.375D, 0.0D, 0.0D,   0.625D, 1.0D,  0.25D ),
		EAST_SHAPE      = VoxelShapes.cuboidUnchecked(0.75D,  0.0D, 0.375D, 1.0D,   1.0D,  0.625D),
		SOUTH_SHAPE     = VoxelShapes.cuboidUnchecked(0.375D, 0.0D, 0.75D,  0.625D, 1.0D,  1.0D  ),
		WEST_SHAPE      = VoxelShapes.cuboidUnchecked(0.0D,   0.0D, 0.375D, 0.25D,  1.0D,  0.625D),
		NORTH_EXTRUSION = VoxelShapes.cuboidUnchecked(0.375D, 0.0D, 0.0D,   0.625D, 0.25D, 0.25D ),
		EAST_EXTRUSION  = VoxelShapes.cuboidUnchecked(0.75D,  0.0D, 0.375D, 1.0D,   0.25D, 0.625D),
		SOUTH_EXTRUSION = VoxelShapes.cuboidUnchecked(0.375D, 0.0D, 0.75D,  0.625D, 0.25D, 1.0D  ),
		WEST_EXTRUSION  = VoxelShapes.cuboidUnchecked(0.0D,   0.0D, 0.375D, 0.25D,  0.25D, 0.625D);

	public SpelunkingRopeBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		BlockPos upPos = pos.up();
		BlockState upState = world.getBlockState(upPos);
		if (upState == state) return;
		if (this.isExtrusionClear(world, upPos, upState, state.get(FACING))) {
			Direction ropeDirection = state.get(FACING);
			BlockPos anchorPos = upPos.offset(ropeDirection);
			BlockState anchorState = world.getBlockState(anchorPos);
			if (
				anchorState.getBlock() == BigGlobeBlocks.ROPE_ANCHOR &&
				anchorState.get(RopeAnchorBlock.HAS_ROPE) &&
				anchorState.get(FACING) == ropeDirection.getOpposite()
			) {
				return;
			}
		}
		this.configureFallingBlockEntity(FallingBlockEntity.spawnFromBlock(world, pos, state));
	}

	@Override
	public void configureFallingBlockEntity(FallingBlockEntity entity) {
		((FallingBlockEntity_DestroyOnLandingAccess)(entity)).setDestroyOnLanding(true);
	}

	@Override
	public void onDestroyedOnLanding(World world, BlockPos pos, FallingBlockEntity fallingBlockEntity) {
		if (world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
			world.spawnEntity(
				new ItemEntity(
					world,
					fallingBlockEntity.getX(),
					fallingBlockEntity.getY(),
					fallingBlockEntity.getZ(),
					new ItemStack(BigGlobeItems.SPELUNKING_ROPE)
				)
			);
		}
	}

	@Override
	public int getFallDelay() {
		return 1;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		if (context instanceof EntityShapeContext entityContext && entityContext.getEntity() instanceof FallingBlockEntity) {
			return VoxelShapes.empty();
		}
		return super.getCollisionShape(state, world, pos, context);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		//no-op.
	}

	@Override
	@Nullable
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockPos upPos = context.getBlockPos().up();
		BlockState upState = context.getWorld().getBlockState(upPos);
		if (upState.getBlock() == this) return upState;
		VoxelShape upShape = upState.getCollisionShape(context.getWorld(), upPos);
		for (Direction direction : Directions.HORIZONTAL) {
			BlockPos sidePos = upPos.offset(direction);
			BlockState sideState = context.getWorld().getBlockState(sidePos);
			if (
				sideState.getBlock() == BigGlobeBlocks.ROPE_ANCHOR &&
				sideState.get(FACING) == direction.getOpposite() &&
				this.isExtrusionClear(upShape, direction)
			) {
				return this.getDefaultState().with(FACING, direction);
			}
		}
		return null;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return switch (state.get(FACING)) {
			case NORTH    -> NORTH_SHAPE;
			case EAST     ->  EAST_SHAPE;
			case SOUTH    -> SOUTH_SHAPE;
			case WEST     ->  WEST_SHAPE;
			case UP, DOWN -> throw new IllegalStateException();
		};
	}

	public boolean isExtrusionClear(WorldView world, BlockPos pos, Direction direction) {
		return this.isExtrusionClear(world, pos, world.getBlockState(pos), direction);
	}

	public boolean isExtrusionClear(WorldView world, BlockPos pos, BlockState state, Direction direction) {
		return this.isExtrusionClear(state.getCollisionShape(world, pos), direction);
	}

	public boolean isExtrusionClear(VoxelShape shape, Direction direction) {
		return !VoxelShapes.matchesAnywhere(
			shape,
			this.getExtrusionShape(direction),
			BooleanBiFunction.AND
		);
	}

	public VoxelShape getExtrusionShape(Direction direction) {
		return switch (direction) {
			case NORTH    -> NORTH_EXTRUSION;
			case EAST     ->  EAST_EXTRUSION;
			case SOUTH    -> SOUTH_EXTRUSION;
			case WEST     ->  WEST_EXTRUSION;
			case UP, DOWN -> throw new IllegalStateException();
		};
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		ItemStack stack = player.getStackInHand(hand);
		if (stack.getItem() == BigGlobeItems.SPELUNKING_ROPE) {
			BlockPos.Mutable mutablePos = pos.mutableCopy().move(0, -1, 0);
			if (this.placeRopesAuto(world, mutablePos, state, player, stack)) {
				this.playPlacementSound(player, world, pos);
				return ActionResult.SUCCESS;
			}
			else {
				return ActionResult.FAIL;
			}
		}
		return ActionResult.PASS;
	}

	public boolean placeRopesAuto(World world, BlockPos.Mutable mutablePos, BlockState toPlace, PlayerEntity player, ItemStack stack) {
		if (world.isClient) {
			return this.placeRopesSimulate(world, mutablePos, toPlace);
		}
		else if (player.isCreative()) {
			return this.placeRopesCreative(world, mutablePos, toPlace);
		}
		else {
			return this.placeRopesSurvival(world, mutablePos, toPlace, stack);
		}
	}

	public boolean placeRopesSurvival(World world, BlockPos.Mutable mutablePos, BlockState toPlace, ItemStack stack) {
		boolean placedAny = false;
		if (!stack.isEmpty()) {
			Chunk chunk = world.getChunk(mutablePos);
			while (chunk.getBlockState(mutablePos) == toPlace) {
				mutablePos.setY(mutablePos.getY() - 1);
			}
			do {
				if (world.isOutOfHeightLimit(mutablePos)) break;
				BlockState toReplace = chunk.getBlockState(mutablePos);
				if (!MaterialVersions.isReplaceable(toReplace)) break;
				world.setBlockState(mutablePos, toPlace, Block.NOTIFY_ALL);
				stack.decrement(1);
				placedAny = true;
				mutablePos.setY(mutablePos.getY() - 1);
			}
			while (!stack.isEmpty());
		}
		return placedAny;
	}

	public boolean placeRopesCreative(World world, BlockPos.Mutable mutablePos, BlockState toPlace) {
		boolean placedAny = false;
		Chunk chunk = world.getChunk(mutablePos);
		while (chunk.getBlockState(mutablePos) == toPlace) {
			mutablePos.setY(mutablePos.getY() - 1);
		}
		while (true) {
			if (world.isOutOfHeightLimit(mutablePos)) break;
			BlockState toReplace = chunk.getBlockState(mutablePos);
			if (!MaterialVersions.isReplaceable(toReplace)) break;
			world.setBlockState(mutablePos, toPlace, Block.NOTIFY_ALL);
			placedAny = true;
			mutablePos.setY(mutablePos.getY() - 1);
		}
		return placedAny;
	}

	public boolean placeRopesSimulate(World world, BlockPos.Mutable mutablePos, BlockState toPlace) {
		Chunk chunk = world.getChunk(mutablePos);
		while (chunk.getBlockState(mutablePos) == toPlace) {
			mutablePos.setY(mutablePos.getY() - 1);
		}
		return (
			!world.isOutOfHeightLimit(mutablePos) &&
			MaterialVersions.isReplaceable(world.getBlockState(mutablePos))
		);
	}

	public void playPlacementSound(PlayerEntity player, World world, BlockPos pos) {
		world.playSound(
			player,
			pos,
			this.soundGroup.getPlaceSound(),
			SoundCategory.BLOCKS,
			this.soundGroup.volume,
			this.soundGroup.pitch
		);
	}

	@Override
	public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.with(FACING, mirror.apply(state.get(FACING)));
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public PistonBehavior getPistonBehavior(BlockState state) {
		return PistonBehavior.DESTROY;
	}
}