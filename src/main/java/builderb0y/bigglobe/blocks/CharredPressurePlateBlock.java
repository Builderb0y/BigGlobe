package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@AddPseudoField("type")

public class CharredPressurePlateBlock extends PressurePlateBlock {

	public static final MapCodec<CharredPressurePlateBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CharredPressurePlateBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

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