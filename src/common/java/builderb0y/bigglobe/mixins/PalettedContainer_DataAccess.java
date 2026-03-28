package builderb0y.bigglobe.mixins;

import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PalettedContainer.class)
public interface PalettedContainer_DataAccess<T> {

	@Accessor("data")
	public abstract PalettedContainer.Data<T> bigglobe_getData();
}