package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.BlockView;

import builderb0y.bigglobe.mixinInterfaces.DimensionalBlockView;

@Mixin(BlockView.class)
public interface BlockView_ExposeDimension extends DimensionalBlockView {

}