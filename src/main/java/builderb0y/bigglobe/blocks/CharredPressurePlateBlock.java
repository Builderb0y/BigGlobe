package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockSetType;
import net.minecraft.block.PressurePlateBlock;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

#if MC_VERSION >= MC_1_20_3
@AddPseudoField("type")
#endif
public class CharredPressurePlateBlock extends PressurePlateBlock {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<CharredPressurePlateBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CharredPressurePlateBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}

		public CharredPressurePlateBlock(BlockSetType type, Settings settings) {
			super(type, settings);
		}

		public BlockSetType type() {
			return this.blockSetType;
		}
	#else

		public CharredPressurePlateBlock(ActivationRule type, Settings settings, BlockSetType blockSetType) {
			super(type, settings, blockSetType);
		}
	#endif

	@Override
	public int getTickRate() {
		return 10;
	}
}