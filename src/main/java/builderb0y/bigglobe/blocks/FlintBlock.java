package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class FlintBlock extends SurfaceMaterialDecorationBlock {

	public static final VoxelShape SHAPE = VoxelShapes.cuboidUnchecked(0.125D, 0.0D, 0.125D, 0.875D, 0.0625D, 0.875D);

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<FlintBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(FlintBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public FlintBlock(Settings settings) {
		super(settings);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}
}