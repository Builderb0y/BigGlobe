package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.chunk.PalettedContainer;

@Mixin(PalettedContainer.class)
public interface PalettedContainer_DataAccess<T> {

	@Accessor("data")
	public abstract PalettedContainer.Data<T> bigglobe_getData();
}