package builderb0y.bigglobe.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class CharredSaplingBlock extends SaplingBlock {

	public CharredSaplingBlock(SaplingGenerator generator, Settings settings) {
		super(generator, settings);
	}

	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
	}
}