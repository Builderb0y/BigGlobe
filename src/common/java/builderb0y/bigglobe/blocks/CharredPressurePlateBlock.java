package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@AddPseudoField("type")
public class CharredPressurePlateBlock extends PressurePlateBlock {

	#if MC_VERSION < MC_26_3_0

		public static final MapCodec<CharredPressurePlateBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CharredPressurePlateBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec codec() {
			return CODEC;
		}

	#endif

	public CharredPressurePlateBlock(BlockSetType type, Properties settings) {
		super(type, settings);
	}

	public BlockSetType type() {
		return this.type;
	}

	@Override
	public int getPressedTime() {
		return 10;
	}
}