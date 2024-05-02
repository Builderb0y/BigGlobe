package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class TallChorusSporeBlock extends TallPlantBlock {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<TallChorusSporeBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(TallChorusSporeBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public TallChorusSporeBlock(Settings settings) {
		super(settings);
	}

	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
	}
}