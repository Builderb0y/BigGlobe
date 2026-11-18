package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class MediumChorusSporeBlock extends ChorusSporeBlock {

	#if MC_VERSION < MC_26_3_0

		public static final MapCodec<MediumChorusSporeBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(MediumChorusSporeBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec codec() {
			return CODEC;
		}

	#endif

	public MediumChorusSporeBlock(Properties settings, Holder<Block> grow_into) {
		super(settings, grow_into);
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}
}