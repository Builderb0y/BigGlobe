package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.BlockStateVersions;

@AddPseudoField("generator")

public class CharredSaplingBlock extends SaplingBlock {

	public static final MapCodec<CharredSaplingBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CharredSaplingBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public TreeGrower generator() {
		return this.treeGrower;
	}

	public CharredSaplingBlock(TreeGrower generator, Properties settings) {
		super(generator, settings);
	}

	@Override
	public boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
		return BlockStateVersions.isOpaqueFullCube(floor, world, pos);
	}
}