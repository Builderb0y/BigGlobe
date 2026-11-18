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

public class ShortChorusSporeBlock extends ChorusSporeBlock {

	public static final VoxelShape SHAPE = Shapes.create(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

	#if MC_VERSION < MC_26_3_0

		public static final MapCodec<ShortChorusSporeBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ShortChorusSporeBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec codec() {
			return CODEC;
		}

	#endif

	public ShortChorusSporeBlock(Properties settings, Holder<Block> grow_into) {
		super(settings, grow_into);
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}