package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlintBlock extends SurfaceMaterialDecorationBlock {

	public static final VoxelShape SHAPE = Shapes.create(0.125D, 0.0D, 0.125D, 0.875D, 0.0625D, 0.875D);

	//don't load BigGlobeAutoCodec too early.
	public static final MapCodec<FlintBlock> CODEC = BlockBehaviour.simpleCodec(FlintBlock::new);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public FlintBlock(Properties settings) {
		super(settings);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}