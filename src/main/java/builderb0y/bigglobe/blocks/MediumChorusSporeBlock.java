package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class MediumChorusSporeBlock extends ChorusSporeBlock {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<MediumChorusSporeBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(MediumChorusSporeBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public MediumChorusSporeBlock(Settings settings, Block grow_into) {
		super(settings, grow_into);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.fullCube();
	}
}