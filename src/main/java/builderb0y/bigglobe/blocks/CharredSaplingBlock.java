package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

#if MC_VERSION >= MC_1_20_3
import net.minecraft.block.SaplingGenerator;
#else
import net.minecraft.block.sapling.SaplingGenerator;
#endif

public class CharredSaplingBlock extends SaplingBlock {

	public CharredSaplingBlock(SaplingGenerator generator, Settings settings) {
		super(generator, settings);
	}

	#if MC_VERSION >= MC_1_20_3
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			throw new NotImplementedException();
		}
	#endif

	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
	}
}