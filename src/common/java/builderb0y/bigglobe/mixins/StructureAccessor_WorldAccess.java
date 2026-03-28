package builderb0y.bigglobe.mixins;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureManager.class)
public interface StructureAccessor_WorldAccess {

	@Accessor("level")
	public abstract LevelAccessor bigglobe_getWorld();
}