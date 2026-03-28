package builderb0y.bigglobe.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VegetationBlock.class)
public interface PlantBlock_CanPlantOnTopAccess {

	@Invoker("mayPlaceOn")
	public abstract boolean bigglobe_canPlantOnTop(BlockState floor, BlockGetter world, BlockPos pos);
}