package builderb0y.bigglobe.blocks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.blockEntities.BigGlobeBlockEntityTypes;
import builderb0y.bigglobe.blockEntities.DelayedGenerationBlockEntity;

public class DelayedGenerationBlock extends Block implements BlockEntityProvider {

	public DelayedGenerationBlock(Settings settings) {
		super(settings);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new DelayedGenerationBlockEntity(BigGlobeBlockEntityTypes.DELAYED_GENERATION, pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (!world.isClient && type == BigGlobeBlockEntityTypes.DELAYED_GENERATION) {
			return (world1, pos, state1, blockEntity) -> ((DelayedGenerationBlockEntity)(blockEntity)).tick();
		}
		return null;
	}
}