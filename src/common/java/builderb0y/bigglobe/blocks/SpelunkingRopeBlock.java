package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

import builderb0y.bigglobe.blockdefs.BigGlobeBlocks;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.mixins.FallingBlockEntity_DestroyOnLandingAccess;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.ActionResultVersions;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.GameruleVersions;

public class SpelunkingRopeBlock extends FallingBlock {

	public static final VoxelShape
		NORTH_SHAPE = Shapes.create(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 0.25D),
		EAST_SHAPE = Shapes.create(0.75D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D),
		SOUTH_SHAPE = Shapes.create(0.375D, 0.0D, 0.75D, 0.625D, 1.0D, 1.0D),
		WEST_SHAPE = Shapes.create(0.0D, 0.0D, 0.375D, 0.25D, 1.0D, 0.625D),
		NORTH_EXTRUSION = Shapes.create(0.375D, 0.0D, 0.0D, 0.625D, 0.25D, 0.25D),
		EAST_EXTRUSION = Shapes.create(0.75D, 0.0D, 0.375D, 1.0D, 0.25D, 0.625D),
		SOUTH_EXTRUSION = Shapes.create(0.375D, 0.0D, 0.75D, 0.625D, 0.25D, 1.0D),
		WEST_EXTRUSION = Shapes.create(0.0D, 0.0D, 0.375D, 0.25D, 0.25D, 0.625D);

	public static final MapCodec<SpelunkingRopeBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(SpelunkingRopeBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public SpelunkingRopeBlock(Properties settings) {
		super(settings);
	}

	@Override
	public int getDustColor(BlockState state, BlockGetter world, BlockPos pos) {
		return 0; //doesn't create particles anyway.
	}

	@Override
	public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		BlockPos upPos = pos.above();
		BlockState upState = world.getBlockState(upPos);
		if (upState == state) return;
		if (this.isExtrusionClear(world, upPos, upState, state.getValue(BlockStateProperties.HORIZONTAL_FACING))) {
			Direction ropeDirection = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			BlockPos anchorPos = upPos.relative(ropeDirection);
			BlockState anchorState = world.getBlockState(anchorPos);
			if (
				anchorState.getBlock() == BigGlobeBlocks.ROPE_ANCHOR &&
				anchorState.getValue(RopeAnchorBlock.HAS_ROPE) &&
				anchorState.getValue(BlockStateProperties.HORIZONTAL_FACING) == ropeDirection.getOpposite()
			) {
				return;
			}
		}
		this.falling(FallingBlockEntity.fall(world, pos, state));
	}

	@Override
	public void falling(FallingBlockEntity entity) {
		((FallingBlockEntity_DestroyOnLandingAccess)(entity)).setDestroyOnLanding(true);
	}

	@Override
	public void onBrokenAfterFall(Level world, BlockPos pos, FallingBlockEntity fallingBlockEntity) {
		if (world instanceof ServerLevel serverWorld && GameruleVersions.tileDrops(serverWorld)) {
			world.addFreshEntity(
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
	public int getDelayAfterPlace() {
		return 1;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof FallingBlockEntity) {
			return Shapes.empty();
		}
		return super.getCollisionShape(state, world, pos, context);
	}

	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		//no-op.
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos upPos = context.getClickedPos().above();
		BlockState upState = context.getLevel().getBlockState(upPos);
		if (upState.getBlock() == this) return upState;
		VoxelShape upShape = upState.getCollisionShape(context.getLevel(), upPos);
		for (Direction direction : Directions.HORIZONTAL) {
			BlockPos sidePos = upPos.relative(direction);
			BlockState sideState = context.getLevel().getBlockState(sidePos);
			if (
				sideState.getBlock() == BigGlobeBlocks.ROPE_ANCHOR &&
				sideState.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction.getOpposite() &&
				this.isExtrusionClear(upShape, direction)
			) {
				return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
			}
		}
		return null;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
			case NORTH -> NORTH_SHAPE;
			case EAST -> EAST_SHAPE;
			case SOUTH -> SOUTH_SHAPE;
			case WEST -> WEST_SHAPE;
			case UP, DOWN -> throw new IllegalStateException();
		};
	}

	public boolean isExtrusionClear(LevelReader world, BlockPos pos, Direction direction) {
		return this.isExtrusionClear(world, pos, world.getBlockState(pos), direction);
	}

	public boolean isExtrusionClear(LevelReader world, BlockPos pos, BlockState state, Direction direction) {
		return this.isExtrusionClear(state.getCollisionShape(world, pos), direction);
	}

	public boolean isExtrusionClear(VoxelShape shape, Direction direction) {
		return !Shapes.joinIsNotEmpty(
			shape,
			this.getExtrusionShape(direction),
			BooleanOp.AND
		);
	}

	public VoxelShape getExtrusionShape(Direction direction) {
		return switch (direction) {
			case NORTH -> NORTH_EXTRUSION;
			case EAST -> EAST_EXTRUSION;
			case SOUTH -> SOUTH_EXTRUSION;
			case WEST -> WEST_EXTRUSION;
			case UP, DOWN -> throw new IllegalStateException();
		};
	}

	@Override

	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

		if (stack.getItem() == BigGlobeItems.SPELUNKING_ROPE) {
			BlockPos.MutableBlockPos mutablePos = pos.mutable().move(0, -1, 0);
			if (this.placeRopesAuto(world, mutablePos, state, player, stack)) {
				this.playPlacementSound(player, world, pos);
				return ActionResultVersions.ITEM_SUCCESS;
			}
			else {
				return ActionResultVersions.ITEM_FAIL;
			}
		}
		return ActionResultVersions.ITEM_PASS;
	}

	public boolean placeRopesAuto(Level world, BlockPos.MutableBlockPos mutablePos, BlockState toPlace, Player player, ItemStack stack) {
		if (world.isClientSide()) {
			return this.placeRopesSimulate(world, mutablePos, toPlace);
		}
		else if (player.isCreative()) {
			return this.placeRopesCreative(world, mutablePos, toPlace);
		}
		else {
			return this.placeRopesSurvival(world, mutablePos, toPlace, stack);
		}
	}

	public boolean placeRopesSurvival(Level world, BlockPos.MutableBlockPos mutablePos, BlockState toPlace, ItemStack stack) {
		boolean placedAny = false;
		if (!stack.isEmpty()) {
			ChunkAccess chunk = world.getChunk(mutablePos);
			while (chunk.getBlockState(mutablePos) == toPlace) {
				mutablePos.setY(mutablePos.getY() - 1);
			}
			do {
				if (world.isOutsideBuildHeight(mutablePos)) break;
				BlockState toReplace = chunk.getBlockState(mutablePos);
				if (!(BlockStateVersions.isReplaceable(toReplace) && toReplace.getFluidState().isEmpty())) break;
				world.setBlock(mutablePos, toPlace, Block.UPDATE_ALL);
				stack.shrink(1);
				placedAny = true;
				mutablePos.setY(mutablePos.getY() - 1);
			}
			while (!stack.isEmpty());
		}
		return placedAny;
	}

	public boolean placeRopesCreative(Level world, BlockPos.MutableBlockPos mutablePos, BlockState toPlace) {
		boolean placedAny = false;
		ChunkAccess chunk = world.getChunk(mutablePos);
		while (chunk.getBlockState(mutablePos) == toPlace) {
			mutablePos.setY(mutablePos.getY() - 1);
		}
		while (true) {
			if (world.isOutsideBuildHeight(mutablePos)) break;
			BlockState toReplace = chunk.getBlockState(mutablePos);
			if (!(BlockStateVersions.isReplaceable(toReplace) && toReplace.getFluidState().isEmpty())) break;
			world.setBlock(mutablePos, toPlace, Block.UPDATE_ALL);
			placedAny = true;
			mutablePos.setY(mutablePos.getY() - 1);
		}
		return placedAny;
	}

	public boolean placeRopesSimulate(Level world, BlockPos.MutableBlockPos mutablePos, BlockState toPlace) {
		ChunkAccess chunk = world.getChunk(mutablePos);
		BlockState prevState;
		while ((prevState = chunk.getBlockState(mutablePos)) == toPlace) {
			mutablePos.setY(mutablePos.getY() - 1);
		}
		return (
			!world.isOutsideBuildHeight(mutablePos) &&
			BlockStateVersions.isReplaceable(prevState) &&
			prevState.getFluidState().isEmpty()
		);
	}

	public void playPlacementSound(Player player, Level world, BlockPos pos) {
		world.playSound(
			player,
			pos,
			this.soundType.getPlaceSound(),
			SoundSource.BLOCKS,
			this.soundType.volume,
			this.soundType.pitch
		);
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(BlockStateProperties.HORIZONTAL_FACING, mirror.mirror(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}
}