package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public interface Entity_PortalCooldownSetter {

	@Accessor("portalCooldown")
	public abstract void bigglobe_setPortalCooldown(int cooldown);
}