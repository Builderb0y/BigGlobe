package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;

@Mixin(StructureManager.class)
public interface StructureAccessor_WorldAccess {

	@Accessor("level")
	public abstract LevelAccessor bigglobe_getWorld();
}