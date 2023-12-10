package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class NetherFlowerBlock extends FlowerBlock {

	public NetherFlowerBlock(StatusEffect suspiciousStewEffect, int effectDuration, Settings settings) {
		super(suspiciousStewEffect, effectDuration, settings);
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