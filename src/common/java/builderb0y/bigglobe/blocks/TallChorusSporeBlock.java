package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class TallChorusSporeBlock extends DoublePlantBlock {

	#if MC_VERSION < MC_26_3_0

		public static final MapCodec<TallChorusSporeBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(TallChorusSporeBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec codec() {
			return CODEC;
		}

	#endif

	public TallChorusSporeBlock(Properties settings) {
		super(settings);
	}

	@Override
	public boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
		return BlockStateVersions.isOpaqueFullCube(floor, world, pos);
	}
}