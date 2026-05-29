package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.blockEntities.BigGlobeBlockEntityTypes;
import builderb0y.bigglobe.blockEntities.DelayedGenerationBlockEntity;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class DelayedGenerationBlock extends Block implements EntityBlock {

	public static final MapCodec<DelayedGenerationBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(DelayedGenerationBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public DelayedGenerationBlock(Properties settings) {
		super(settings);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DelayedGenerationBlockEntity(BigGlobeBlockEntityTypes.DELAYED_GENERATION, pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		if (!world.isClientSide() && type == BigGlobeBlockEntityTypes.DELAYED_GENERATION) {
			return (Level world1, BlockPos pos, BlockState state1, T blockEntity) -> ((DelayedGenerationBlockEntity)(blockEntity)).tick();
		}
		return null;
	}
}