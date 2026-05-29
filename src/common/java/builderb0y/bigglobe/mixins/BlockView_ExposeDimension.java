package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.BlockGetter;

import builderb0y.bigglobe.mixinInterfaces.DimensionalBlockView;

@Mixin(BlockGetter.class)
public interface BlockView_ExposeDimension extends DimensionalBlockView {}