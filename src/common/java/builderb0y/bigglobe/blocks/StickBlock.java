package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StickBlock extends SurfaceMaterialDecorationBlock {

	#if MC_VERSION < MC_26_3_0

		//don't classload BigGlobeAutoCodec too early.
		public static final MapCodec<StickBlock> CODEC = BlockBehaviour.simpleCodec(StickBlock::new);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec codec() {
			return CODEC;
		}

	#endif

	public static final VoxelShape SHAPE = Shapes.create(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);

	public StickBlock(Properties settings) {
		super(settings);
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}