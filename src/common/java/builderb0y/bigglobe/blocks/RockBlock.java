package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.itemdefs.BigGlobeItems;

public class RockBlock extends SurfaceMaterialDecorationBlock {

	public static final VoxelShape SHAPE = Shapes.create(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
	public static final IntegerProperty ROCKS = IntegerProperty.create("rocks", 1, 6);

	public static final MapCodec<RockBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(RockBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public RockBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(ROCKS, 1));
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return context.getItemInHand().is(BigGlobeItems.ROCK) && state.getValue(ROCKS) < 6;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		int rocks;
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		if (state.is(this) && (rocks = state.getValue(ROCKS)) < 6) {
			return state.setValue(ROCKS, rocks + 1);
		}
		else {
			return this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).isSourceOfType(Fluids.WATER));
		}
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ROCKS);
	}
}