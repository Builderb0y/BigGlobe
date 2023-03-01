package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

@Mixin(PlantBlock.class)
public interface PlantBlock_CanPlantOnTopAccess {

	@Invoker("canPlantOnTop")
	public abstract boolean bigglobe_canPlantOnTop(BlockState floor, BlockView world, BlockPos pos);
}