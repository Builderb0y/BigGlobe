package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.StructureAccessor;

@Mixin(StructureAccessor.class)
public interface StructureAccessor_WorldAccess {

	@Accessor("world")
	public abstract WorldAccess bigglobe_getWorld();
}