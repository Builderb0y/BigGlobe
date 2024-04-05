package builderb0y.bigglobe.blocks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.shape.VoxelShape;

import builderb0y.bigglobe.items.BigGlobeItems;

public class RockBlock extends SurfaceMaterialDecorationBlock {

	public static final IntProperty ROCKS = IntProperty.of("rocks", 1, 6);

	public RockBlock(Settings settings, VoxelShape shape) {
		super(settings, shape);
		this.setDefaultState(this.getDefaultState().with(ROCKS, 1));
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