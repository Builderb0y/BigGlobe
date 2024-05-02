package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.items.BigGlobeItems;

public class RockBlock extends SurfaceMaterialDecorationBlock {

	public static final VoxelShape SHAPE = VoxelShapes.cuboidUnchecked(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
	public static final IntProperty ROCKS = IntProperty.of("rocks", 1, 6);

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<RockBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(RockBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public RockBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(ROCKS, 1));
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public boolean canReplace(BlockState state, ItemPlacementContext context) {
		return context.getStack().isOf(BigGlobeItems.ROCK) && state.get(ROCKS) < 6;
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
		int rocks;
		BlockState state = context.getWorld().getBlockState(context.getBlockPos());
		if (state.isOf(this) && (rocks = state.get(ROCKS)) < 6) {
			return state.with(ROCKS, rocks + 1);
		}
		else {
			return this.getDefaultState().with(Properties.WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).isEqualAndStill(Fluids.WATER));
		}
	}

	@Override
	public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(ROCKS);
	}
}