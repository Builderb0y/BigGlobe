package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import builderb0y.bigglobe.mixinInterfaces.DimensionalBlockView;
import net.minecraft.world.level.BlockGetter;

@Mixin(BlockGetter.class)
public interface BlockView_ExposeDimension extends DimensionalBlockView {

}