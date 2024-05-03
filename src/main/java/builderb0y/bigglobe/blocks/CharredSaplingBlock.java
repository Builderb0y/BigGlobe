package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

#if MC_VERSION >= MC_1_20_3
import net.minecraft.block.SaplingGenerator;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
#else
import net.minecraft.block.sapling.SaplingGenerator;

import builderb0y.autocodec.annotations.AddPseudoField;
#endif

#if MC_VERSION >= MC_1_20_3
@AddPseudoField("generator")
#endif
public class CharredSaplingBlock extends SaplingBlock {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<CharredSaplingBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CharredSaplingBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}

		public SaplingGenerator generator() {
			return this.generator;
		}
	#endif

	public CharredSaplingBlock(SaplingGenerator generator, Settings settings) {
		super(generator, settings);
	}


	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
	}
}